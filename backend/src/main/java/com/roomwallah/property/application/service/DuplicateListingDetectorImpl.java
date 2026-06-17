package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.property.domain.entity.SuspectedDuplicateCluster;
import com.roomwallah.property.domain.repository.SuspectedDuplicateClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateListingDetectorImpl {

    private final PropertyRepository propertyRepository;
    private final SuspectedDuplicateClusterRepository clusterRepository;

    @Transactional
    public void scanAndFlagDuplicates(Property newProperty) {
        log.info("Scanning for duplicates of new property: {}", newProperty.getId());
        List<Property> candidates = propertyRepository.findAll().stream()
                .filter(p -> !p.getId().equals(newProperty.getId()))
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .toList();

        for (Property candidate : candidates) {
            double score = calculateSimilarityScore(newProperty, candidate);
            if (score >= 0.80) { // 80% similarity threshold
                log.warn("Detected high similarity duplicate cluster between new property: {} and existing: {}. Score: {}",
                        newProperty.getId(), candidate.getId(), score);

                List<String> insights = new ArrayList<>();
                if (getGeospatialDistance(newProperty, candidate) < 100) {
                    insights.add("Coordinates proximity distance < 100m");
                }
                if (getTextSimilarity(newProperty.getDescription(), candidate.getDescription()) > 0.85) {
                    insights.add("Description cosine similarity > 85%");
                }
                if (newProperty.getOwnerId().equals(candidate.getOwnerId())) {
                    insights.add("Matching owners");
                } else {
                    insights.add("Cross-owner duplicates (Potential fraud alert)");
                }

                String clusterId = "cluster-" + UUID.randomUUID().toString().substring(0, 8);
                SuspectedDuplicateCluster cluster = SuspectedDuplicateCluster.builder()
                        .id(clusterId)
                        .similarityScore(score * 100.0)
                        .locality(newProperty.getAddress() != null ? newProperty.getAddress().getLine2() : "Unknown")
                        .city(newProperty.getAddress() != null ? newProperty.getAddress().getCity() : "Unknown")
                        .candidateA(candidate) // Older / existing
                        .candidateB(newProperty) // Newer
                        .matchInsights(String.join(", ", insights))
                        .status("PENDING")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                clusterRepository.save(cluster);
                
                // Do NOT automatically reject the listing, flag it for human verification
                newProperty.setModerationStatus("UNDER_DUPLICATE_REVIEW");
                newProperty.setModerationReason("Flagged by AI Duplicate Engine. Score: " + (score * 100) + "%");
                propertyRepository.save(newProperty);
            }
        }
    }

    public double calculateSimilarityScore(Property p1, Property p2) {
        double locationWeight = 0.35;
        double textWeight = 0.45;
        double priceWeight = 0.20;

        double locationScore = 0.0;
        double distance = getGeospatialDistance(p1, p2);
        if (distance < 50) {
            locationScore = 1.0;
        } else if (distance < 200) {
            locationScore = 0.8;
        } else if (distance < 1000) {
            locationScore = 0.4;
        }

        double textScore = getTextSimilarity(p1.getDescription(), p2.getDescription());
        double titleScore = getTextSimilarity(p1.getTitle(), p2.getTitle());
        double overallTextScore = (textScore * 0.7) + (titleScore * 0.3);

        double priceScore = 0.0;
        if (p1.getPrice() != null && p2.getPrice() != null) {
            BigDecimal amt1 = p1.getPrice().getAmount();
            BigDecimal amt2 = p2.getPrice().getAmount();
            if (amt1 != null && amt2 != null && amt1.compareTo(BigDecimal.ZERO) > 0) {
                double diff = amt1.subtract(amt2).abs().doubleValue() / amt1.doubleValue();
                if (diff < 0.02) priceScore = 1.0;
                else if (diff < 0.05) priceScore = 0.8;
                else if (diff < 0.15) priceScore = 0.4;
            }
        }

        return (locationScore * locationWeight) + (overallTextScore * textWeight) + (priceScore * priceWeight);
    }

    private double getGeospatialDistance(Property p1, Property p2) {
        if (p1.getGeoLocation() == null || p2.getGeoLocation() == null) {
            return Double.MAX_VALUE;
        }
        BigDecimal lat1 = p1.getGeoLocation().getLatitude();
        BigDecimal lon1 = p1.getGeoLocation().getLongitude();
        BigDecimal lat2 = p2.getGeoLocation().getLatitude();
        BigDecimal lon2 = p2.getGeoLocation().getLongitude();

        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        double r = 6371e3; // meters
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double deltaLambda = Math.toRadians(lon2.subtract(lon1).doubleValue());

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return r * c; // in meters
    }

    private double getTextSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String[] w1 = s1.toLowerCase().split("\\W+");
        String[] w2 = s2.toLowerCase().split("\\W+");

        Set<String> set1 = new HashSet<>(Arrays.asList(w1));
        Set<String> set2 = new HashSet<>(Arrays.asList(w2));

        int intersectSize = 0;
        for (String w : set1) {
            if (set2.contains(w)) {
                intersectSize++;
            }
        }
        int unionSize = set1.size() + set2.size() - intersectSize;
        return unionSize > 0 ? ((double) intersectSize / unionSize) : 0.0;
    }
}
