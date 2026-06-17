package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.VerificationDecisionAudit;
import java.util.List;
import java.util.UUID;

public interface VerificationHistoryService {
    List<VerificationDecisionAudit> getHistory(UUID requestId);
}
