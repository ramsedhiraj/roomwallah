package com.roomwallah.verification.presentation;

import com.roomwallah.common.dto.ApiResponse;
import com.roomwallah.exception.RateLimitExceededException;
import com.roomwallah.identity.application.service.CurrentUserProvider;
import com.roomwallah.security.limiter.RedisRateLimiter;
import com.roomwallah.user.entity.User;
import com.roomwallah.verification.application.facade.VerificationFacade;
import com.roomwallah.verification.domain.entity.*;
import com.roomwallah.verification.presentation.dto.*;
import com.roomwallah.verification.application.service.OtpService;
import com.roomwallah.verification.application.service.PropertyVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Verification", description = "Verification, trust scores, and anti-broker auditing endpoints")
public class VerificationController {

    private final VerificationFacade verificationFacade;
    private final CurrentUserProvider currentUserProvider;
    private final RedisRateLimiter redisRateLimiter;
    private final OtpService otpService;
    private final PropertyVerificationService propertyVerificationService;

    @PostMapping("/api/v1/verifications/identity")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit identity verification details (DigiLocker, Aadhaar, PAN, etc.)")
    public ApiResponse<VerificationRequestResponseDto> submitIdentityVerification(
            @Valid @RequestBody IdentityVerificationRequestDto requestDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        User currentUser = currentUserProvider.getCurrentUser();
        UUID userId = currentUser.getId();

        log.info("Identity verification submission request for user: {} with provider: {}", userId, requestDto.getProvider());

        // Enforce rate limiting: 3 submission attempts per minute
        String rateLimitKey = "verification_submit:" + userId;
        if (!redisRateLimiter.isAllowed(rateLimitKey, 3, 60)) {
            log.warn("Rate limit exceeded for user: {} on identity verification submission", userId);
            throw new RateLimitExceededException("Too many verification attempts. Please wait a minute and try again.");
        }

        VerificationProvider provider;
        try {
            provider = VerificationProvider.valueOf(requestDto.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid verification provider: " + requestDto.getProvider());
        }

        VerificationRequest request = verificationFacade.submit(userId, provider, requestDto.getCode(), idempotencyKey);
        VerificationRequestResponseDto responseDto = mapToResponse(request);

        return ApiResponse.success(responseDto, "Verification request submitted successfully");
    }

    @GetMapping("/api/v1/verifications/active")
    @Operation(summary = "Get the active or latest verification request of the current user")
    public ApiResponse<VerificationRequestResponseDto> getActiveVerification() {
        User currentUser = currentUserProvider.getCurrentUser();
        UUID userId = currentUser.getId();

        log.info("Fetching active verification request for user: {}", userId);
        VerificationRequest request = verificationFacade.getActiveVerification(userId);
        if (request == null) {
            return ApiResponse.success(null, "No active verification request found");
        }

        return ApiResponse.success(mapToResponse(request), "Active verification request retrieved");
    }

    @GetMapping("/api/v1/verifications/trust-score")
    @Operation(summary = "Get the current authenticated user's trust score details")
    public ApiResponse<TrustScoreResponseDto> getMyTrustScore() {
        User currentUser = currentUserProvider.getCurrentUser();
        UUID userId = currentUser.getId();

        log.info("Fetching trust score for user: {}", userId);
        TrustScore trustScore = verificationFacade.getTrustScore(userId);
        if (trustScore == null) {
            return ApiResponse.success(null, "No trust score calculated yet");
        }

        TrustScoreResponseDto responseDto = TrustScoreResponseDto.builder()
                .overallScore(trustScore.getOverallScore())
                .identityScore(trustScore.getIdentityScore())
                .propertyScore(trustScore.getPropertyScore())
                .reviewScore(trustScore.getReviewScore())
                .activityScore(trustScore.getActivityScore())
                .fraudPenalty(trustScore.getFraudPenalty())
                .calculatedAt(trustScore.getCalculatedAt())
                .build();

        return ApiResponse.success(responseDto, "Trust score details retrieved");
    }

    @GetMapping("/api/v1/verifications/fraud-signals")
    @Operation(summary = "Get fraud/broker signals raised against the current authenticated user")
    public ApiResponse<List<FraudSignalResponseDto>> getMyFraudSignals() {
        User currentUser = currentUserProvider.getCurrentUser();
        UUID userId = currentUser.getId();

        log.info("Fetching fraud signals for user: {}", userId);
        List<FraudSignal> signals = verificationFacade.getFraudSignals(userId);
        List<FraudSignalResponseDto> dtoList = signals.stream()
                .map(this::mapToFraudSignalDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtoList, "User fraud signals retrieved");
    }

    // ==========================================
    // Administrative Endpoints (Admin role only)
    // ==========================================

    @GetMapping("/api/v1/admin/verifications/pending")
    @Operation(summary = "Get all verification requests currently in pending status")
    public ApiResponse<List<VerificationRequestResponseDto>> getPendingRequests() {
        log.info("Admin fetching pending verification requests");
        List<VerificationRequest> requests = verificationFacade.getPendingRequests();
        List<VerificationRequestResponseDto> dtoList = requests.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(dtoList, "Pending requests retrieved");
    }

    @GetMapping("/api/v1/admin/verifications/fraud-signals")
    @Operation(summary = "Get all system-wide broker and fraud signals")
    public ApiResponse<List<FraudSignalResponseDto>> getAllFraudSignals() {
        log.info("Admin fetching all system fraud signals");
        List<FraudSignal> signals = verificationFacade.getAllFraudSignals();
        List<FraudSignalResponseDto> dtoList = signals.stream()
                .map(this::mapToFraudSignalDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtoList, "All fraud signals retrieved");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/approve")
    @Operation(summary = "Approve a verification request (Transition to APPROVED)")
    public ApiResponse<VerificationRequestResponseDto> approveRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} approving verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.approve(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request approved");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/reject")
    @Operation(summary = "Reject a verification request (Transition to REJECTED)")
    public ApiResponse<VerificationRequestResponseDto> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} rejecting verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.reject(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request rejected");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/escalate")
    @Operation(summary = "Escalate a verification request (Transition to ESCALATED)")
    public ApiResponse<VerificationRequestResponseDto> escalateRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} escalating verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.escalate(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request escalated");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/reopen")
    @Operation(summary = "Reopen an expired or rejected request for fresh review")
    public ApiResponse<VerificationRequestResponseDto> reopenRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} reopening verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.reopen(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request reopened");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/revoke")
    @Operation(summary = "Revoke an approved verification request (Transition to REJECTED/REVOKED status)")
    public ApiResponse<VerificationRequestResponseDto> revokeRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} revoking verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.revoke(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request revoked");
    }

    @PostMapping("/api/v1/admin/verifications/{id}/expire")
    @Operation(summary = "Force expiration of an approved verification request (Transition to EXPIRED)")
    public ApiResponse<VerificationRequestResponseDto> expireRequest(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        User admin = currentUserProvider.getCurrentUser();
        log.info("Admin {} expiring verification request {}", admin.getId(), id);
        VerificationRequest request = verificationFacade.expire(id, admin.getId(), decisionDto.getReason());
        return ApiResponse.success(mapToResponse(request), "Verification request expired");
    }

    @GetMapping("/api/v1/admin/verifications/{id}/history")
    @Operation(summary = "Get the audit trail of decisions for a specific verification request")
    public ApiResponse<List<VerificationDecisionAuditResponseDto>> getDecisionHistory(@PathVariable UUID id) {
        log.info("Fetching audit trail of decisions for verification request {}", id);
        List<VerificationDecisionAudit> audits = verificationFacade.getDecisionHistory(id);
        List<VerificationDecisionAuditResponseDto> dtoList = audits.stream()
                .map(this::mapToDecisionAuditDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtoList, "Verification request audit history retrieved");
    }

    @PostMapping("/api/v1/verifications/email/request")
    @Operation(summary = "Request email verification OTP")
    public ApiResponse<Void> requestEmailOtp(@RequestBody(required = false) OtpRequestDto requestDto) {
        User currentUser = currentUserProvider.getCurrentUser();
        String target = (requestDto != null && requestDto.getTarget() != null && !requestDto.getTarget().isBlank())
                ? requestDto.getTarget() : currentUser.getEmail();
        otpService.generateOtp(currentUser.getId(), target, "EMAIL_OTP");
        return ApiResponse.success(null, "Verification code sent to email successfully");
    }

    @PostMapping("/api/v1/verifications/email/verify")
    @Operation(summary = "Verify email OTP")
    public ApiResponse<Void> verifyEmailOtp(@Valid @RequestBody OtpVerificationRequestDto requestDto) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean success = otpService.verifyOtp(currentUser.getId(), requestDto.getCode(), "EMAIL_OTP");
        if (!success) {
            throw new IllegalArgumentException("Invalid or expired email verification code");
        }
        return ApiResponse.success(null, "Email verified successfully");
    }

    @PostMapping("/api/v1/verifications/mobile/request")
    @Operation(summary = "Request mobile phone verification OTP")
    public ApiResponse<Void> requestMobileOtp(@RequestBody(required = false) OtpRequestDto requestDto) {
        User currentUser = currentUserProvider.getCurrentUser();
        String target = (requestDto != null && requestDto.getTarget() != null && !requestDto.getTarget().isBlank())
                ? requestDto.getTarget() : currentUser.getPhone();
        otpService.generateOtp(currentUser.getId(), target, "MOBILE_OTP");
        return ApiResponse.success(null, "Verification code sent to phone number successfully");
    }

    @PostMapping("/api/v1/verifications/mobile/verify")
    @Operation(summary = "Verify mobile phone OTP")
    public ApiResponse<Void> verifyMobileOtp(@Valid @RequestBody OtpVerificationRequestDto requestDto) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean success = otpService.verifyOtp(currentUser.getId(), requestDto.getCode(), "MOBILE_OTP");
        if (!success) {
            throw new IllegalArgumentException("Invalid or expired mobile verification code");
        }
        return ApiResponse.success(null, "Mobile phone number verified successfully");
    }

    @PostMapping("/api/v1/verifications/property")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit property deeds and utility bills for automatic & manual verification")
    public ApiResponse<PropertyVerificationResponseDto> submitPropertyVerification(
            @Valid @RequestBody PropertyVerificationRequestDto requestDto
    ) {
        log.info("Property verification submission for property: {}", requestDto.getPropertyId());
        PropertyVerification pv = propertyVerificationService.submitPropertyVerification(
                requestDto.getPropertyId(),
                requestDto.getDocumentUrl(),
                requestDto.getUtilityBillUrl(),
                requestDto.getOwnerNameOnDeed(),
                requestDto.getAddressOnUtilityBill(),
                requestDto.getOwnerNameOnUtilityBill()
        );
        return ApiResponse.success(mapToPropertyResponse(pv), "Property verification submitted successfully");
    }

    @GetMapping("/api/v1/verifications/property/{propertyId}/status")
    @Operation(summary = "Get verification status of a specific property listing")
    public ApiResponse<PropertyVerificationResponseDto> getPropertyVerificationStatus(
            @PathVariable UUID propertyId
    ) {
        log.info("Fetching verification status for property: {}", propertyId);
        PropertyVerification pv = propertyVerificationService.getPropertyVerification(propertyId);
        if (pv == null) {
            return ApiResponse.success(null, "No verification details found for this property");
        }
        return ApiResponse.success(mapToPropertyResponse(pv), "Property verification status retrieved");
    }

    @GetMapping("/api/v1/admin/verifications/properties/pending")
    @Operation(summary = "Get all property verifications pending manual admin review")
    public ApiResponse<List<PropertyVerificationResponseDto>> getPendingPropertyVerifications() {
        log.info("Admin fetching pending property verifications");
        List<PropertyVerification> list = propertyVerificationService.getPendingPropertyVerifications();
        List<PropertyVerificationResponseDto> dtoList = list.stream()
                .map(this::mapToPropertyResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(dtoList, "Pending property verifications retrieved");
    }

    @PostMapping("/api/v1/admin/verifications/properties/{verificationId}/approve")
    @Operation(summary = "Approve a property verification request manually")
    public ApiResponse<PropertyVerificationResponseDto> approvePropertyVerification(
            @PathVariable UUID verificationId,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        log.info("Admin manually approving property verification: {}", verificationId);
        PropertyVerification pv = propertyVerificationService.approvePropertyVerification(
                verificationId,
                decisionDto.getReason()
        );
        return ApiResponse.success(mapToPropertyResponse(pv), "Property verification approved successfully");
    }

    @PostMapping("/api/v1/admin/verifications/properties/{verificationId}/reject")
    @Operation(summary = "Reject a property verification request manually")
    public ApiResponse<PropertyVerificationResponseDto> rejectPropertyVerification(
            @PathVariable UUID verificationId,
            @Valid @RequestBody AdminDecisionRequestDto decisionDto
    ) {
        log.info("Admin manually rejecting property verification: {}", verificationId);
        PropertyVerification pv = propertyVerificationService.rejectPropertyVerification(
                verificationId,
                decisionDto.getReason()
        );
        return ApiResponse.success(mapToPropertyResponse(pv), "Property verification rejected");
    }

    // ==========================================
    // Mapping Helper Methods
    // ==========================================

    private PropertyVerificationResponseDto mapToPropertyResponse(PropertyVerification pv) {
        if (pv == null) return null;
        return PropertyVerificationResponseDto.builder()
                .id(pv.getId())
                .propertyId(pv.getPropertyId())
                .ownerId(pv.getOwnerId())
                .documentUrl(pv.getDocumentUrl())
                .utilityBillUrl(pv.getUtilityBillUrl())
                .deedNameMatched(pv.isDeedNameMatched())
                .utilityNameMatched(pv.isUtilityNameMatched())
                .locationMatched(pv.isLocationMatched())
                .confidenceScore(pv.getConfidenceScore())
                .approvalStatus(pv.getApprovalStatus())
                .rejectionReason(pv.getRejectionReason())
                .verifiedAt(pv.getVerifiedAt())
                .build();
    }

    private VerificationRequestResponseDto mapToResponse(VerificationRequest request) {
        return VerificationRequestResponseDto.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .provider(request.getProvider().name())
                .requestStatus(request.getRequestStatus().name())
                .verifiedName(request.getVerifiedName())
                .confidenceScore(request.getConfidenceScore())
                .submittedAt(request.getSubmittedAt())
                .completedAt(request.getCompletedAt())
                .expiresAt(request.getExpiresAt())
                .rejectionReason(request.getRejectionReason())
                .verificationVersion(request.getVerificationVersion())
                .build();
    }

    private FraudSignalResponseDto mapToFraudSignalDto(FraudSignal signal) {
        return FraudSignalResponseDto.builder()
                .id(signal.getId())
                .userId(signal.getUserId())
                .signalType(signal.getSignalType())
                .severity(signal.getSeverity() != null ? signal.getSeverity().name() : null)
                .brokerRiskScore(signal.getBrokerRiskScore())
                .description(signal.getDescription())
                .createdAt(signal.getCreatedAt())
                .build();
    }

    private VerificationDecisionAuditResponseDto mapToDecisionAuditDto(VerificationDecisionAudit audit) {
        return VerificationDecisionAuditResponseDto.builder()
                .id(audit.getId())
                .verificationRequestId(audit.getVerificationRequestId())
                .adminId(audit.getAdminId())
                .previousStatus(audit.getPreviousStatus())
                .newStatus(audit.getNewStatus())
                .decisionReason(audit.getDecisionReason())
                .correlationId(audit.getCorrelationId())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}
