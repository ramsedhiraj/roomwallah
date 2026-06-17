package com.roomwallah.verification.application.service;

import com.roomwallah.common.observability.CorrelationContext;
import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.media.domain.entity.MediaType;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.verification.domain.entity.*;
import com.roomwallah.verification.domain.port.EventPublisherPort;
import com.roomwallah.verification.domain.port.TrustScorePolicyPort;
import com.roomwallah.verification.domain.repository.FraudSignalRepository;
import com.roomwallah.verification.domain.repository.TrustScoreRepository;
import com.roomwallah.verification.domain.repository.VerificationBadgeRepository;
import com.roomwallah.verification.domain.valueobject.TrustBreakdown;
import com.roomwallah.verification.infrastructure.cache.VerificationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreServiceImpl implements TrustScoreService {

    private final UserRepository userRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final VerificationBadgeRepository badgeRepository;
    private final FraudSignalRepository fraudSignalRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final TrustScorePolicyPort policyPort;
    private final VerificationCacheService cacheService;
    private final EventPublisherPort eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public TrustScore calculateAndSave(UUID userId) {
        log.info("Calculating trust score for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 1. Calculate scores based on configurable policy weights
        int identityScore = user.isIdentityVerified() ? policyPort.getIdentityPoints() : 0;
        int phoneScore = user.isPhoneVerified() ? policyPort.getPhonePoints() : 0;
        int emailScore = user.isEmailVerified() ? policyPort.getEmailPoints() : 0;

        // Property Verification Points
        List<Property> activeProperties = propertyRepository.findByOwnerIdAndDeletedFalse(userId);
        long activeCount = activeProperties.stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .count();
        int propertyScore = activeCount > 0 ? policyPort.getPropertyPoints() : 0;

        // Video Walkthrough Points
        boolean hasVideo = false;
        for (Property prop : activeProperties) {
            long videoCount = mediaRepository.countByPropertyIdAndMediaTypeAndDeletedFalse(prop.getId(), MediaType.VIDEO);
            if (videoCount > 0) {
                hasVideo = true;
                break;
            }
        }
        int videoScore = hasVideo ? policyPort.getVideoWalkthroughPoints() : 0;

        // Verified Reviews (Mock check - default to 20 if owner has active properties, or 0)
        int reviewScore = activeCount > 0 ? policyPort.getReviewPoints() : 0;

        // Fraud Penalty
        List<FraudSignal> signals = fraudSignalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int penalty = !signals.isEmpty() ? policyPort.getFraudPenalty() : 0;

        // 2. Sum and Clamp Overall Score (0 - 100)
        int rawScore = identityScore + phoneScore + emailScore + propertyScore + videoScore + reviewScore + penalty;
        int overallScore = Math.max(0, Math.min(100, rawScore));

        log.info("Trust Score breakdown for user {}: Identity={}, Phone={}, Email={}, Property={}, Video={}, Review={}, Penalty={} -> Overall={}",
                userId, identityScore, phoneScore, emailScore, propertyScore, videoScore, reviewScore, penalty, overallScore);

        // 3. Save Trust Score Entity
        TrustScore trustScore = trustScoreRepository.findByUserId(userId)
                .orElseGet(() -> {
                    TrustScore score = new TrustScore();
                    score.setUserId(userId);
                    return score;
                });

        trustScore.setOverallScore(overallScore);
        trustScore.setIdentityScore(identityScore);
        trustScore.setPropertyScore(propertyScore + videoScore); // Combined Property and Video Walkthrough
        trustScore.setReviewScore(reviewScore);
        trustScore.setActivityScore(phoneScore + emailScore); // Combined Phone and Email Activities
        trustScore.setFraudPenalty(penalty);
        trustScore.setCalculatedAt(Instant.now(clock));

        trustScore = trustScoreRepository.save(trustScore);

        // 4. Badge Evaluation
        evaluateAndAwardBadge(userId, overallScore);

        // 5. Invalidate Caches
        cacheService.evictAll(userId);

        // 6. Publish score changed event to Outbox
        eventPublisher.publish(com.roomwallah.verification.domain.event.TrustScoreChangedEvent.class.cast(
            new com.roomwallah.verification.domain.event.TrustScoreChangedEvent(
                userId,
                overallScore,
                CorrelationContext.get(),
                Instant.now(clock)
            )
        ));

        return trustScore;
    }

    @Override
    @Transactional(readOnly = true)
    public TrustScore getTrustScore(UUID userId) {
        return trustScoreRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trust score not found for user ID: " + userId));
    }

    private void evaluateAndAwardBadge(UUID userId, int overallScore) {
        Optional<BadgeLevel> targetBadgeOpt = Optional.empty();
        if (overallScore >= policyPort.getDiamondThreshold()) {
            targetBadgeOpt = Optional.of(BadgeLevel.DIAMOND);
        } else if (overallScore >= policyPort.getGoldThreshold()) {
            targetBadgeOpt = Optional.of(BadgeLevel.GOLD);
        } else if (overallScore >= policyPort.getSilverThreshold()) {
            targetBadgeOpt = Optional.of(BadgeLevel.SILVER);
        } else if (overallScore >= policyPort.getBronzeThreshold()) {
            targetBadgeOpt = Optional.of(BadgeLevel.BRONZE);
        }

        Optional<VerificationBadge> badgeOpt = badgeRepository.findByUserId(userId);

        if (targetBadgeOpt.isPresent()) {
            BadgeLevel targetLevel = targetBadgeOpt.get();
            if (badgeOpt.isPresent()) {
                VerificationBadge existingBadge = badgeOpt.get();
                if (existingBadge.getBadgeLevel() != targetLevel) {
                    existingBadge.setBadgeLevel(targetLevel);
                    existingBadge.setAwardedAt(Instant.now(clock));
                    badgeRepository.save(existingBadge);
                    publishBadgeEvent(userId, targetLevel);
                }
            } else {
                VerificationBadge newBadge = new VerificationBadge();
                newBadge.setUserId(userId);
                newBadge.setBadgeLevel(targetLevel);
                newBadge.setAwardedAt(Instant.now(clock));
                badgeRepository.save(newBadge);
                publishBadgeEvent(userId, targetLevel);
            }
        } else {
            // Overall score dropped below Bronze threshold, revoke badge
            badgeOpt.ifPresent(badge -> {
                badgeRepository.delete(badge);
                log.info("Revoked verification badge for user: {}", userId);
            });
        }
    }

    private void publishBadgeEvent(UUID userId, BadgeLevel level) {
        eventPublisher.publish(new com.roomwallah.verification.domain.event.BadgeAwardedEvent(
            userId,
            level,
            CorrelationContext.get(),
            Instant.now(clock)
        ));
    }
}
