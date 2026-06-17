package com.roomwallah.identity.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessTokenServiceImpl implements AccessTokenService {

    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret:super_secret_jwt_signing_key_that_is_at_least_256_bits_long_for_security}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 mins default
    private long accessTokenExpirationMs;

    @Override
    public String generateToken(User user) {
        try {
            Instant now = Instant.now(clock);
            Instant expiry = now.plusMillis(accessTokenExpirationMs);

            Map<String, String> header = Map.of("alg", "HS256", "typ", "JWT");
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));

            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", user.getId().toString());
            claims.put("role", user.getRole().name());
            claims.put("jti", UUID.randomUUID().toString());
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", expiry.getEpochSecond());

            String encodedClaims = base64UrlEncode(objectMapper.writeValueAsString(claims));

            String signInput = encodedHeader + "." + encodedClaims;
            String signature = calculateHmacSha256(signInput, jwtSecret);

            return signInput + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate access token", e);
            throw new IllegalStateException("Could not generate token", e);
        }
    }

    @Override
    public String extractUserId(String token) {
        return (String) extractClaim(token, "sub");
    }

    @Override
    public String extractRole(String token) {
        return (String) extractClaim(token, "role");
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Number exp = (Number) extractClaim(token, "exp");
            if (exp == null) return true;
            Instant expiry = Instant.ofEpochSecond(exp.longValue());
            return expiry.isBefore(Instant.now(clock));
        } catch (Exception e) {
            return true;
        }
    }

    private Object extractClaim(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<?, ?> claims = objectMapper.readValue(claimsJson, Map.class);
            return claims.get(claimName);
        } catch (Exception e) {
            log.warn("Failed to extract claim {} from token", claimName);
            return null;
        }
    }

    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] rawHmac = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
