package com.roomwallah.identity.presentation.controller;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.identity.application.service.IdentityFacade;
import com.roomwallah.identity.domain.port.AuthType;
import com.roomwallah.identity.presentation.dto.AuthResponse;
import com.roomwallah.identity.presentation.dto.LoginRequest;
import com.roomwallah.identity.presentation.dto.RefreshRequest;
import com.roomwallah.identity.presentation.dto.RegisterRequest;
import com.roomwallah.identity.presentation.dto.UserProfileResponse;
import com.roomwallah.identity.presentation.dto.ForgotPasswordRequest;
import com.roomwallah.identity.presentation.dto.ResetPasswordRequest;
import com.roomwallah.identity.presentation.dto.VerifyEmailRequest;
import com.roomwallah.identity.presentation.dto.LoginOtpRequest;
import com.roomwallah.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and profile endpoints")
public class AuthController {

    private final IdentityFacade identityFacade;
    private final com.roomwallah.common.security.ZeroTrustService zeroTrustService;


    @PostMapping("/register")
    @Operation(summary = "Register a new owner or tenant")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        User user = identityFacade.register(request);
        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
        return ApiResponse.success(response, "User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue tokens")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info("Received login request for identity: {}", request.getIdentity());

        String userAgent = httpServletRequest.getHeader("User-Agent");
        String ipAddress = httpServletRequest.getRemoteAddr();
        String browser = parseBrowser(userAgent);
        String os = parseOs(userAgent);
        String deviceName = browser + " on " + os;

        AuthResponse authResponse = identityFacade.login(
                request.getIdentity(),
                request.getPassword(),
                AuthType.PASSWORD,
                deviceName,
                browser,
                os,
                ipAddress
        );

        return ApiResponse.success(authResponse, "Login successful");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Received token refresh request");
        AuthResponse authResponse = identityFacade.refresh(request.getRefreshToken());
        return ApiResponse.success(authResponse, "Token refreshed successfully");
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke user session and logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        log.info("Received logout request");
        identityFacade.logout(request.getRefreshToken());
        return ApiResponse.success("Logged out successfully");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ApiResponse<UserProfileResponse> me() {
        log.info("Received get profile request for current authenticated user");
        UserProfileResponse profile = identityFacade.getProfile();
        return ApiResponse.success(profile, "User profile retrieved successfully");
    }

    @GetMapping("/session-risk")
    @Operation(summary = "Get current session risk evaluation details")
    public ApiResponse<com.roomwallah.common.security.SessionRiskEvaluator.RiskEvaluationResult> sessionRisk(HttpServletRequest request) {
        return ApiResponse.success(zeroTrustService.getCurrentSessionRisk(request), "Session risk evaluated successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset OTP code")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Received forgot password request for email: {}", request.getEmail());
        identityFacade.forgotPassword(request.getEmail());
        return ApiResponse.success(null, "Password reset code sent successfully");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP code")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received reset password request for email: {}", request.getEmail());
        identityFacade.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.success(null, "Password reset successfully");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email using OTP code")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Received email verification request for email: {}", request.getEmail());
        identityFacade.verifyEmail(request.getEmail(), request.getCode());
        return ApiResponse.success(null, "Email verified successfully");
    }

    @PostMapping("/login/otp/request")
    @Operation(summary = "Request OTP code for logging in")
    public ApiResponse<Void> requestLoginOtp(@Valid @RequestBody LoginOtpRequest request) {
        log.info("Received request for login OTP code for email: {}", request.getEmail());
        identityFacade.requestLoginOtp(request.getEmail());
        return ApiResponse.success(null, "Login OTP code sent successfully");
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Edg")) return "Edge";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Firefox")) return "Firefox";
        return "Other";
    }

    private String parseOs(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "macOS";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        if (userAgent.contains("Linux")) return "Linux";
        return "Other";
    }
}
