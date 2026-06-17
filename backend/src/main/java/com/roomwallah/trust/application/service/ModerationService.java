package com.roomwallah.trust.application.service;

import com.roomwallah.trust.domain.entity.ModerationCase;
import com.roomwallah.trust.domain.entity.ModerationStatus;
import com.roomwallah.trust.domain.port.ModerationCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ModerationService {

    private final ModerationCaseRepository moderationCaseRepository;

    public ModerationService(ModerationCaseRepository moderationCaseRepository) {
        this.moderationCaseRepository = moderationCaseRepository;
    }

    @Transactional
    public ModerationCase createCase(String entityType, UUID entityId, BigDecimal priorityScore) {
        log.info("Creating moderation case for: {} with ID: {} and priority: {}", entityType, entityId, priorityScore);
        ModerationCase moderationCase = ModerationCase.builder()
                .entityType(entityType)
                .entityId(entityId)
                .status(ModerationStatus.OPEN)
                .priorityScore(priorityScore != null ? priorityScore : BigDecimal.ZERO)
                .build();
        return moderationCaseRepository.save(moderationCase);
    }

    @Transactional
    public ModerationCase assignAdmin(UUID caseId, UUID adminId) {
        log.info("Assigning admin: {} to moderation case: {}", adminId, caseId);
        ModerationCase moderationCase = moderationCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation case not found: " + caseId));

        moderationCase.setAssignedAdmin(adminId);
        moderationCase.setStatus(ModerationStatus.IN_PROGRESS);
        return moderationCaseRepository.save(moderationCase);
    }

    @Transactional
    public ModerationCase resolveCase(UUID caseId, String notes) {
        log.info("Resolving moderation case: {}", caseId);
        ModerationCase moderationCase = moderationCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Moderation case not found: " + caseId));

        moderationCase.setStatus(ModerationStatus.RESOLVED);
        moderationCase.setClosedAt(Instant.now());
        moderationCase.setNotes(notes);
        return moderationCaseRepository.save(moderationCase);
    }

    public List<ModerationCase> getOpenCases() {
        return moderationCaseRepository.findByStatus(ModerationStatus.OPEN);
    }
}
