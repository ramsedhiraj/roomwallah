package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.VerificationProvider;
import com.roomwallah.verification.domain.entity.VerificationRequest;
import java.util.UUID;

public interface VerificationSubmissionService {
    VerificationRequest submit(UUID userId, VerificationProvider provider, String code, String idempotencyKey);
    VerificationRequest getActiveVerification(UUID userId);
}
