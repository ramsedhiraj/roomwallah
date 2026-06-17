package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingHealthScoreServiceImpl {

    private final PropertyRepository propertyRepository;

    public Map<String, Object> calculateHealthScore(Property property) {
        int score = 0;
        List<String> suggestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Title Completeness (15 points)
        if (property.getTitle() != null && !property.getTitle().trim().isEmpty()) {
            int words = property.getTitle().trim().split("\\s+").length;
            if (words >= 8) {
                score += 15;
            } else if (words >= 4) {
                score += 10;
                suggestions.add("Add a more descriptive title (aim for 8+ words).");
            } else {
                score += 5;
                suggestions.add("Your title is too short. Descriptive titles attract more views.");
            }
        } else {
            suggestions.add("Title is missing.");
        }

        // 2. Description Completeness (25 points)
        if (property.getDescription() != null && !property.getDescription().trim().isEmpty()) {
            int chars = property.getDescription().length();
            if (chars >= 150) {
                score += 25;
            } else if (chars >= 50) {
                score += 15;
                suggestions.add("Expand your description with details on transit options, nearby landmarks, or rules.");
            } else {
                score += 8;
                suggestions.add("Description is extremely short. Detailed descriptions build trust.");
            }
        } else {
            suggestions.add("Description is missing.");
        }

        // 3. Address details (15 points)
        if (property.getAddress() != null) {
            boolean hasLine1 = property.getAddress().getLine1() != null && !property.getAddress().getLine1().isEmpty();
            boolean hasZip = property.getAddress().getZipCode() != null && !property.getAddress().getZipCode().isEmpty();
            if (hasLine1 && hasZip) {
                score += 15;
            } else {
                score += 10;
                suggestions.add("Complete your address lines and postal code.");
            }
        } else {
            suggestions.add("Address fields are missing.");
        }

        // 4. Amenities details (15 points)
        if (property.getAmenities() != null && !property.getAmenities().isEmpty()) {
            int count = property.getAmenities().size();
            if (count >= 5) {
                score += 15;
            } else if (count >= 2) {
                score += 10;
                suggestions.add("List more amenities like Wifi, AC, lift, or parking to stand out.");
            } else {
                score += 5;
                suggestions.add("Add amenities to make your listing discoverable in searches.");
            }
        } else {
            suggestions.add("No amenities are selected.");
        }

        // 5. Rooms/Beds details (15 points)
        if (property.getBedrooms() != null && property.getBathrooms() != null) {
            score += 15;
        } else {
            score += 5;
            suggestions.add("Specify number of bedrooms and bathrooms.");
        }

        // 6. Price validation (15 points)
        if (property.getPrice() != null && property.getPrice().getAmount() != null 
                && property.getPrice().getAmount().compareTo(BigDecimal.ZERO) > 0) {
            score += 15;
        } else {
            suggestions.add("Price is missing or invalid.");
        }

        // 7. Pricing validation compared to locality averages
        if (property.getAddress() != null && property.getPrice() != null && property.getPrice().getAmount() != null) {
            BigDecimal localityAvg = getLocalityAveragePrice(property.getAddress().getCity(), property.getAddress().getLine2());
            if (localityAvg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentPrice = property.getPrice().getAmount();
                double deviation = currentPrice.subtract(localityAvg).abs().doubleValue() / localityAvg.doubleValue();
                if (deviation > 0.50) {
                    if (currentPrice.compareTo(localityAvg) > 0) {
                        warnings.add("Price is over 50% higher than the average for " + property.getAddress().getLine2() + " (Avg: INR " + localityAvg.setScale(0, RoundingMode.HALF_UP) + ")");
                    } else {
                        warnings.add("Price is over 50% lower than the average for " + property.getAddress().getLine2() + ". This might look suspicious to tenants.");
                    }
                }
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("propertyId", property.getId());
        report.put("completenessScore", score);
        report.put("suggestions", suggestions);
        report.put("warnings", warnings);
        report.put("isHealthy", score >= 75 && warnings.isEmpty());
        return report;
    }

    private BigDecimal getLocalityAveragePrice(String city, String locality) {
        if (city == null || locality == null) return BigDecimal.ZERO;
        
        List<Property> properties = propertyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .filter(p -> p.getAddress() != null && city.equalsIgnoreCase(p.getAddress().getCity()) 
                        && locality.equalsIgnoreCase(p.getAddress().getLine2()))
                .toList();

        if (properties.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Property p : properties) {
            if (p.getPrice() != null && p.getPrice().getAmount() != null) {
                sum = sum.add(p.getPrice().getAmount());
                count++;
            }
        }
        
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
