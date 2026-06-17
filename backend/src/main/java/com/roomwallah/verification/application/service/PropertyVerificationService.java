package com.roomwallah.verification.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.user.entity.User;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.verification.domain.entity.PropertyVerification;
import com.roomwallah.verification.domain.repository.PropertyVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyVerificationService {

    private final PropertyVerificationRepository verificationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public PropertyVerification submitPropertyVerification(
            UUID propertyId,
            String documentUrl,
            String utilityBillUrl,
            String ownerNameOnDeed,
            String addressOnUtilityBill,
            String ownerNameOnUtilityBill
    ) {
        log.info("Submitting property verification for property: {}", propertyId);

        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + propertyId));

        User owner = userRepository.findById(property.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Property owner not found"));

        // Default inputs if not provided explicitly
        String nameOnDeed = (ownerNameOnDeed != null && !ownerNameOnDeed.isBlank()) ? ownerNameOnDeed : owner.getFullName();
        String nameOnUtility = (ownerNameOnUtilityBill != null && !ownerNameOnUtilityBill.isBlank()) ? ownerNameOnUtilityBill : owner.getFullName();
        
        String propAddressStr = "";
        if (property.getAddress() != null) {
            propAddressStr = String.format("%s %s %s %s",
                    Objects.toString(property.getAddress().getLine1(), ""),
                    Objects.toString(property.getAddress().getLine2(), ""),
                    Objects.toString(property.getAddress().getCity(), ""),
                    Objects.toString(property.getAddress().getZipCode(), "")).trim();
        }
        String utilityAddress = (addressOnUtilityBill != null && !addressOnUtilityBill.isBlank()) ? addressOnUtilityBill : propAddressStr;

        // 1. Ownership Document Check (Jaccard similarity against owner profile name)
        double deedSimilarity = calculateJaccardSimilarity(owner.getFullName(), nameOnDeed);
        boolean deedNameMatched = deedSimilarity >= 0.70;

        // 2. Utility Bill Name & Address Check
        double utilityNameSimilarity = calculateJaccardSimilarity(owner.getFullName(), nameOnUtility);
        boolean utilityNameMatched = utilityNameSimilarity >= 0.70;

        double utilityAddressSimilarity = calculateJaccardSimilarity(propAddressStr, utilityAddress);
        boolean utilityAddressMatched = utilityAddressSimilarity >= 0.50; // address strings can be noisy

        // 3. Location Checks (Validate GPS coordinates against city latitude/longitude boundaries)
        Double latitude = (property.getGeoLocation() != null && property.getGeoLocation().getLatitude() != null)
                ? property.getGeoLocation().getLatitude().doubleValue() : null;
        Double longitude = (property.getGeoLocation() != null && property.getGeoLocation().getLongitude() != null)
                ? property.getGeoLocation().getLongitude().doubleValue() : null;

        boolean locationMatched = checkCoordinatesInCity(
                property.getAddress() != null ? property.getAddress().getCity() : null,
                latitude,
                longitude
        );

        // 4. Calculate Confidence Score (0-100)
        // Deed Match: max 35 points
        double deedScore = deedSimilarity >= 0.70 ? 35.0 : deedSimilarity * 35.0;
        // Utility Name/Address Match: max 30 points
        double utilityScore = ((utilityNameSimilarity * 0.5) + (utilityAddressSimilarity * 0.5)) * 30.0;
        // Location Match: max 35 points
        double locationScore = locationMatched ? 35.0 : 0.0;

        double totalScoreVal = Math.min(100.0, Math.max(0.0, deedScore + utilityScore + locationScore));
        BigDecimal confidenceScore = BigDecimal.valueOf(totalScoreVal).setScale(2, RoundingMode.HALF_UP);

        // Determine status: automates approvals for scores >= 70%; flags lower scores for manual review queue.
        String approvalStatus = "PENDING";
        Instant verifiedAt = null;
        if (totalScoreVal >= 70.0) {
            approvalStatus = "APPROVED";
            verifiedAt = Instant.now();
        } else if (totalScoreVal < 40.0) {
            approvalStatus = "REJECTED";
        }

        // Save or update verification details
        PropertyVerification verification = verificationRepository.findByPropertyId(propertyId)
                .orElse(new PropertyVerification());
        verification.setPropertyId(propertyId);
        verification.setOwnerId(owner.getId());
        verification.setDocumentUrl(documentUrl);
        verification.setUtilityBillUrl(utilityBillUrl);
        verification.setDeedNameMatched(deedNameMatched);
        verification.setUtilityNameMatched(utilityNameMatched && utilityAddressMatched);
        verification.setLocationMatched(locationMatched);
        verification.setConfidenceScore(confidenceScore);
        verification.setApprovalStatus(approvalStatus);
        verification.setVerifiedAt(verifiedAt);
        if (verification.getVersion() == null) {
            verification.setVersion(0L);
        }

        PropertyVerification saved = verificationRepository.save(verification);
        log.info("Property verification logged. Status: {}, Score: {}", approvalStatus, confidenceScore);
        return saved;
    }

    @Transactional(readOnly = true)
    public PropertyVerification getPropertyVerification(UUID propertyId) {
        return verificationRepository.findByPropertyId(propertyId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PropertyVerification> getPendingPropertyVerifications() {
        return verificationRepository.findByApprovalStatus("PENDING");
    }

    @Transactional
    public PropertyVerification approvePropertyVerification(UUID verificationId, String reason) {
        PropertyVerification pv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found: " + verificationId));
        pv.setApprovalStatus("APPROVED");
        pv.setVerifiedAt(Instant.now());
        pv.setRejectionReason(reason); // Reuse to store review notes
        return verificationRepository.save(pv);
    }

    @Transactional
    public PropertyVerification rejectPropertyVerification(UUID verificationId, String reason) {
        PropertyVerification pv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found: " + verificationId));
        pv.setApprovalStatus("REJECTED");
        pv.setRejectionReason(reason);
        return verificationRepository.save(pv);
    }

    private double calculateJaccardSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isBlank() || s2.isBlank()) return 0.0;
        
        // Split into words, normalize to lowercase
        String[] tokens1 = s1.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        String[] tokens2 = s2.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");

        Set<String> set1 = new HashSet<>(Arrays.asList(tokens1));
        Set<String> set2 = new HashSet<>(Arrays.asList(tokens2));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private boolean checkCoordinatesInCity(String city, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return false;

        // Coordinate bounding boxes for major cities (lat/lon)
        if (city == null || city.isBlank()) {
            // General India box check
            return latitude >= 8.0 && latitude <= 37.0 && longitude >= 68.0 && longitude <= 97.0;
        }

        String normalizedCity = city.toLowerCase().trim();
        switch (normalizedCity) {
            case "mumbai":
                return latitude >= 18.8 && latitude <= 19.3 && longitude >= 72.7 && longitude <= 73.1;
            case "bangalore":
            case "bengaluru":
                return latitude >= 12.8 && latitude <= 13.1 && longitude >= 77.4 && longitude <= 77.8;
            case "delhi":
            case "new delhi":
                return latitude >= 28.4 && latitude <= 28.9 && longitude >= 76.8 && longitude <= 77.4;
            case "pune":
                return latitude >= 18.4 && latitude <= 18.7 && longitude >= 73.7 && longitude <= 74.1;
            case "chennai":
                return latitude >= 12.8 && latitude <= 13.2 && longitude >= 80.1 && longitude <= 80.4;
            case "hyderabad":
                return latitude >= 17.2 && latitude <= 17.6 && longitude >= 78.2 && longitude <= 78.6;
            default:
                // General India box check
                return latitude >= 8.0 && latitude <= 37.0 && longitude >= 68.0 && longitude <= 97.0;
        }
    }
}
