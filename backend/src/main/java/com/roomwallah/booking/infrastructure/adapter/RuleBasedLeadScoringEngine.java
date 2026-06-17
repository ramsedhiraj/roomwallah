package com.roomwallah.booking.infrastructure.adapter;

import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.domain.entity.VisitStatus;
import com.roomwallah.booking.domain.port.LeadScoringPort;
import com.roomwallah.booking.domain.repository.PropertyVisitRepository;
import com.roomwallah.booking.domain.valueobject.LeadScoreExplanation;
import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.domain.port.TrustScoreRepository;
import com.roomwallah.verification.domain.entity.BadgeLevel;
import com.roomwallah.verification.domain.entity.VerificationBadge;
import com.roomwallah.verification.domain.repository.VerificationBadgeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RuleBasedLeadScoringEngine implements LeadScoringPort {

    private final TrustScoreRepository trustScoreRepository;
    private final VerificationBadgeRepository verificationBadgeRepository;
    private final PropertyVisitRepository propertyVisitRepository;

    @Autowired
    public RuleBasedLeadScoringEngine(
            TrustScoreRepository trustScoreRepository,
            VerificationBadgeRepository verificationBadgeRepository,
            PropertyVisitRepository propertyVisitRepository) {
        this.trustScoreRepository = trustScoreRepository;
        this.verificationBadgeRepository = verificationBadgeRepository;
        this.propertyVisitRepository = propertyVisitRepository;
    }

    @Override
    public LeadScoreExplanation calculateLeadScore(UUID tenantId, UUID ownerId) {
        log.info("Calculating lead score for tenantId: {}, ownerId: {}", tenantId, ownerId);

        int score = 50; // Base score
        List<String> details = new ArrayList<>();
        details.add("Base score: 50");

        // 1. Evaluate Trust Score
        int trustScoreVal = 50; // Neutral default
        try {
            Optional<TrustScore> trustScoreOpt = trustScoreRepository.findByUserId(tenantId);
            if (trustScoreOpt.isPresent()) {
                trustScoreVal = trustScoreOpt.get().getCurrentScore();
                details.add("Found tenant trust score: " + trustScoreVal);
            } else {
                details.add("No trust score found, using default: 50");
            }
        } catch (Exception e) {
            log.warn("Error loading trust score for tenant: {}, defaulting to 50", tenantId, e);
            details.add("Error loading trust score, using default: 50");
        }

        // Trust score adjustment: (trustScoreVal - 50) / 2
        int trustAdjustment = (trustScoreVal - 50) / 2;
        score += trustAdjustment;
        details.add(String.format("Trust score adjustment: %s%d", trustAdjustment >= 0 ? "+" : "", trustAdjustment));

        // 2. Evaluate Verification Badge Level
        try {
            Optional<VerificationBadge> badgeOpt = verificationBadgeRepository.findByUserId(tenantId);
            if (badgeOpt.isPresent()) {
                BadgeLevel level = badgeOpt.get().getBadgeLevel();
                int badgeBonus = 0;
                switch (level) {
                    case DIAMOND:
                        badgeBonus = 25;
                        break;
                    case GOLD:
                        badgeBonus = 20;
                        break;
                    case SILVER:
                        badgeBonus = 15;
                        break;
                    case BRONZE:
                        badgeBonus = 10;
                        break;
                }
                score += badgeBonus;
                details.add("Tenant verification level " + level + ": +" + badgeBonus);
            } else {
                details.add("No verification badge found: +0");
            }
        } catch (Exception e) {
            log.warn("Error loading verification badge for tenant: {}, defaulting to 0 bonus", tenantId, e);
            details.add("Error loading verification badge, using default: +0");
        }

        // 3. Evaluate Visit History (Completions & No-shows)
        try {
            List<PropertyVisit> visits = propertyVisitRepository.findByTenantId(tenantId);
            long completedCount = visits.stream()
                    .filter(v -> v.getStatus() == VisitStatus.COMPLETED)
                    .count();
            long noShowCount = visits.stream()
                    .filter(v -> v.getStatus() == VisitStatus.NO_SHOW)
                    .count();

            // Visit completion bonus (5 points each, max 20)
            int completionBonus = Math.min((int) (completedCount * 5), 20);
            score += completionBonus;
            details.add(String.format("Completed visits (%d): +%d (max 20)", completedCount, completionBonus));

            // No-show penalty (-20 points each)
            int noShowPenalty = (int) (noShowCount * 20);
            score -= noShowPenalty;
            details.add(String.format("No-show visits (%d): -%d", noShowCount, noShowPenalty));
        } catch (Exception e) {
            log.warn("Error loading visit history for tenant: {}, defaulting to no visits", tenantId, e);
            details.add("Error loading visit history, defaulting to no adjustment");
        }

        // Clamp score between 0 and 100
        int finalScore = Math.max(0, Math.min(score, 100));
        details.add("Final score clamped to range [0-100]");

        String explanation = String.join(" | ", details);
        log.info("Lead score calculation completed. Score: {}, Explanation: {}", finalScore, explanation);

        return new LeadScoreExplanation(finalScore, explanation);
    }
}
