package com.roomwallah.identity.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.identity.application.service.IdentityFacade;
import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.identity.presentation.dto.LoginRequest;
import com.roomwallah.identity.presentation.dto.RefreshRequest;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityFacade identityFacade;

    @Test
    void register_withValidBody_returnsSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phone("+919876543210")
                .password("Password123!")
                .role(UserRole.TENANT)
                .build();

        User mockUser = new User();
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john.doe@example.com");
        mockUser.setPhone("+919876543210");
        mockUser.setRole(UserRole.TENANT);

        when(identityFacade.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(identityFacade).register(any(RegisterRequest.class));
    }

    @Test
    void login_withValidBody_returnsTokens() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .identity("john.doe@example.com")
                .password("Password123!")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("access_token_123")
                .refreshToken("refresh_token_123")
                .tokenType("Bearer")
                .role("TENANT")
                .email("john.doe@example.com")
                .build();

        when(identityFacade.login(
                eq("john.doe@example.com"),
                eq("Password123!"),
                eq(AuthType.PASSWORD),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("User-Agent", "Mozilla/5.0")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token_123"));
    }

    @Test
    void refresh_withValidBody_returnsNewTokens() throws Exception {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("old_refresh_token")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .tokenType("Bearer")
                .role("TENANT")
                .email("john.doe@example.com")
                .build();

        when(identityFacade.refresh("old_refresh_token")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
    }
}
