package com.roomwallah.security.config;

import com.roomwallah.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.roomwallah.security.filter.CorrelationIdFilter correlationIdFilter;
    private final com.roomwallah.partner.filter.ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final com.roomwallah.security.filter.ZeroTrustFilter zeroTrustFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/health",
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/session-risk",
                    "/api/v1/owners/*/public-profile",
                    "/api/v1/media/properties/*",
                    "/api/v1/media/files/**",
                    "/api/v1/search",
                    "/api/v1/search/autocomplete",
                    "/api/v1/search/trending",
                    "/api/v1/webhooks/payments/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/api/v1/admin/payments/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/trust/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/search/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/bookings/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/v1/admin/leads/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/v1/admin/calendar/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/v1/admin/visits/**").hasAnyRole("ADMIN", "OWNER")
                .requestMatchers("/api/v1/partner/**").hasRole("PARTNER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/properties/me").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/properties/*").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(zeroTrustFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Cache-Control",
                "X-Correlation-ID",
                "Correlation-ID",
                "X-Tenant-ID",
                "Idempotency-Key",
                "X-Nonce",
                "X-Timestamp",
                "X-Device-Location",
                "X-Partner-Key"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
