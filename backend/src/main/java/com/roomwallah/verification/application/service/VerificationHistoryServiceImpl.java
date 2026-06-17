package com.roomwallah.verification.application.service;

import com.roomwallah.verification.domain.entity.VerificationDecisionAudit;
import com.roomwallah.verification.domain.repository.VerificationDecisionAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationHistoryServiceImpl implements VerificationHistoryService {

    private final VerificationDecisionAuditRepository decisionAuditRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VerificationDecisionAudit> getHistory(UUID requestId) {
        return decisionAuditRepository.findByVerificationRequestIdOrderByCreatedAtAsc(requestId);
    }
}
