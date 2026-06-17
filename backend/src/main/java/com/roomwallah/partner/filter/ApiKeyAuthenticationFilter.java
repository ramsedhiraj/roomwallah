package com.roomwallah.partner.filter;

import com.roomwallah.partner.domain.PartnerApiKey;
import com.roomwallah.partner.service.ApiKeyValidationResult;
import com.roomwallah.partner.service.PartnerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final PartnerService partnerService;

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader("X-Partner-Key");
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Authenticating partner API key...");
        ApiKeyValidationResult result = partnerService.validateApiKey(apiKey);

        if (result == ApiKeyValidationResult.VALID) {
            String hash = sha256(apiKey);
            PartnerApiKey keyEntity = partnerService.getApiKeyByHashCached(hash);
            
            if (keyEntity != null) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(keyEntity.getScopes().split(","))
                        .map(String::trim)
                        .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.toUpperCase()))
                        .collect(Collectors.toList());
                authorities.add(new SimpleGrantedAuthority("ROLE_PARTNER"));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        keyEntity.getPartnerName(),
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Partner authenticated: {}", keyEntity.getPartnerName());
            }
            filterChain.doFilter(request, response);
        } else if (result == ApiKeyValidationResult.QUOTA_EXCEEDED) {
            response.setStatus(429);
            response.getWriter().write("Daily quota limit exceeded.");
        } else {
            response.setStatus(401);
            response.getWriter().write("Invalid or expired partner API key.");
        }
    }
}
