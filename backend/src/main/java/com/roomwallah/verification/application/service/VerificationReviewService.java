package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.VerificationRequest;
import java.util.List;
import java.util.UUID;

public interface VerificationReviewService {
    VerificationRequest approve(UUID requestId, UUID adminId, String reason);
    VerificationRequest reject(UUID requestId, UUID adminId, String reason);
    VerificationRequest escalate(UUID requestId, UUID adminId, String reason);
    VerificationRequest reopen(UUID requestId, UUID adminId, String reason);
    VerificationRequest revoke(UUID requestId, UUID adminId, String reason);
    VerificationRequest expire(UUID requestId, UUID adminId, String reason);
    List<VerificationRequest> getPendingRequests();
}
