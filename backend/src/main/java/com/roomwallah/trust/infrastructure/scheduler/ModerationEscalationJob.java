package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.domain.entity.ModerationCase;
import com.roomwallah.trust.domain.entity.ModerationStatus;
import com.roomwallah.trust.domain.port.ModerationCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class ModerationEscalationJob {

    private final ModerationCaseRepository moderationCaseRepository;

    public ModerationEscalationJob(ModerationCaseRepository moderationCaseRepository) {
        this.moderationCaseRepository = moderationCaseRepository;
    }

    @Scheduled(cron = "${roomwallah.trust.moderation-escalate.cron:0 0/15 * * * ?}")
    @Transactional
    public void run() {
        log.info("Starting ModerationEscalationJob...");
        List<ModerationCase> openCases = moderationCaseRepository.findByStatus(ModerationStatus.OPEN);
        Instant now = Instant.now();

        for (ModerationCase c : openCases) {
            try {
                Duration age = Duration.between(c.getCreatedAt(), now);
                if (age.toHours() > 24) {
                    BigDecimal oldPriority = c.getPriorityScore();
                    BigDecimal newPriority = oldPriority.add(new BigDecimal("1.5000"));
                    c.setPriorityScore(newPriority);
                    moderationCaseRepository.save(c);
                    log.info("Escalated case ID: {} priority from {} to {}", c.getId(), oldPriority, newPriority);
                }
            } catch (Exception e) {
                log.error("Failed to escalate moderation case ID: {}", c.getId(), e);
            }
        }
        log.info("Finished ModerationEscalationJob.");
    }
}
