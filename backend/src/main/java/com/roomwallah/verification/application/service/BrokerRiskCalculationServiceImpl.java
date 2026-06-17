package com.roomwallah.verification.application.service;

import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.verification.domain.entity.VerificationRequest;
import com.roomwallah.verification.domain.entity.VerificationRequestStatus;
import com.roomwallah.verification.domain.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerRiskCalculationServiceImpl implements BrokerRiskCalculationService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final VerificationRequestRepository requestRepository;

    @Value("${roomwallah.broker.policy.max-listings:3}")
    private int maxListingsLimit;

    @Override
    public int calculateRiskScore(UUID userId) {
        log.info("Calculating broker risk score for user: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return 0;
        }
        User user = userOpt.get();
        int riskScore = 0;

        // 1. Excessive Active Listings Check (Limit: Configurable, default 3)
        long activeCount = propertyRepository.countByOwnerIdAndStatusAndDeletedFalse(userId, PropertyStatus.ACTIVE);
        if (activeCount > maxListingsLimit) {
            log.warn("User {} exceeds active listings limit: {}/{}", userId, activeCount, maxListingsLimit);
            riskScore += 30;
        }

        // 2. Duplicate Verified Identity Check
        Optional<VerificationRequest> activeReqOpt = requestRepository
                .findFirstByUserIdAndRequestStatusOrderByCreatedAtDesc(userId, VerificationRequestStatus.VERIFIED);
        if (activeReqOpt.isPresent()) {
            VerificationRequest activeReq = activeReqOpt.get();
            if (activeReq.getProviderReference() != null && !activeReq.getProviderReference().isBlank()) {
                // Find if another user has a verified request with same provider reference
                List<VerificationRequest> duplicates = requestRepository
                        .findByRequestStatusOrderByCreatedAtDesc(VerificationRequestStatus.VERIFIED)
                        .stream()
                        .filter(r -> !r.getUserId().equals(userId) && activeReq.getProviderReference().equals(r.getProviderReference()))
                        .collect(Collectors.toList());
                if (!duplicates.isEmpty()) {
                    log.warn("User {} shares verified identity ref {} with other users", userId, activeReq.getProviderReference());
                    riskScore += 40;
                }
            }
        }

        // 3. Repeated Phone Numbers Check
        // Phone numbers in RoomWallah are unique at database level, but we can check if there are multiple user accounts sharing name patterns or similar metadata, or mock checking phone number sharing in case of soft deletes
        List<User> allUsers = userRepository.findAll();
        long phoneMatches = allUsers.stream()
                .filter(u -> !u.getId().equals(userId) && user.getPhone().equals(u.getPhone()))
                .count();
        if (phoneMatches > 0) {
            log.warn("User {} shares phone number with other user accounts", userId);
            riskScore += 20;
        }

        // 4. Duplicate Media Checksum Check
        List<Property> userProperties = propertyRepository.findByOwnerIdAndDeletedFalse(userId);
        boolean duplicateMediaFound = false;
        for (Property prop : userProperties) {
            List<PropertyMedia> mediaList = mediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(prop.getId());
            for (PropertyMedia media : mediaList) {
                if (media.getChecksum() != null && media.getChecksum().getChecksumSha256() != null) {
                    String sha256 = media.getChecksum().getChecksumSha256();
                    // Query if another owner's property has same media checksum
                    List<PropertyMedia> identicalMedias = mediaRepository.findAll().stream()
                            .filter(m -> !m.isDeleted() 
                                      && sha256.equals(m.getChecksum() != null ? m.getChecksum().getChecksumSha256() : null))
                            .collect(Collectors.toList());
                    
                    for (PropertyMedia match : identicalMedias) {
                        Property matchProp = propertyRepository.findById(match.getPropertyId()).orElse(null);
                        if (matchProp != null && !matchProp.getOwnerId().equals(userId)) {
                            duplicateMediaFound = true;
                            break;
                        }
                    }
                }
                if (duplicateMediaFound) break;
            }
            if (duplicateMediaFound) break;
        }

        if (duplicateMediaFound) {
            log.warn("User {} has duplicate media hashes shared with another owner's listings", userId);
            riskScore += 30;
        }

        // Clamp to 0 - 100
        int finalScore = Math.max(0, Math.min(100, riskScore));
        log.info("Calculated broker risk score for user {}: {}", userId, finalScore);
        return finalScore;
    }
}
