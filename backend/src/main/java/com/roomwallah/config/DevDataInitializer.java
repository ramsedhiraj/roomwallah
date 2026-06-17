package com.roomwallah.config;

import com.roomwallah.user.entity.User;
import com.roomwallah.user.entity.UserRole;
import com.roomwallah.user.entity.AccountStatus;
import com.roomwallah.user.repository.UserRepository;
import com.roomwallah.property.domain.entity.*;
import com.roomwallah.property.domain.valueobject.*;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.search.application.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final SearchIndexService searchIndexService;

    @Override
    public void run(String... args) {
        try {
            log.info("Starting development data seeding...");
            
            // 1. Create owner Rohan Owner if not exists
            User owner = userRepository.findByEmail("owner@roomwallah.com").orElse(null);
            if (owner == null) {
                owner = new User();
                owner.setFullName("John Owner");
                owner.setEmail("owner@roomwallah.com");
                owner.setPhone("+919999999999");
                owner.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));
                owner.setRole(UserRole.OWNER);
                owner.setStatus(AccountStatus.ACTIVE);
                owner.setEmailVerified(true);
                owner.setPhoneVerified(true);
                owner.setIdentityVerified(true);
                owner = userRepository.save(owner);
                log.info("Seeded dev owner: owner@roomwallah.com");
            }

            // 2. Create tenant Rohan Tenant if not exists
            User tenant = userRepository.findByEmail("rohan@tenant.com").orElse(null);
            if (tenant == null) {
                tenant = new User();
                tenant.setFullName("Rohan Tenant");
                tenant.setEmail("rohan@tenant.com");
                tenant.setPhone("+918888888888");
                tenant.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));
                tenant.setRole(UserRole.TENANT);
                tenant.setStatus(AccountStatus.ACTIVE);
                tenant.setEmailVerified(true);
                tenant.setPhoneVerified(true);
                tenant.setIdentityVerified(true);
                tenant = userRepository.save(tenant);
                log.info("Seeded dev tenant: rohan@tenant.com");
            }

            // 3. Create Mumbai properties
            if (propertyRepository.count() == 0) {
                // Property 1: Luxury Apartment in Bandra
                Property luxuryProp = new Property();
                luxuryProp.setTitle("Luxury Apartment in Bandra");
                luxuryProp.setDescription("Stunning 2 BHK luxury apartment with modern amenities, sea view, and dedicated parking.");
                luxuryProp.setOwnerId(owner.getId());
                luxuryProp.setPropertyType(PropertyType.APARTMENT);
                luxuryProp.setListingPurpose(ListingPurpose.RENT);
                luxuryProp.setStatus(PropertyStatus.ACTIVE);
                luxuryProp.setVisibility(PropertyVisibility.PUBLIC);
                luxuryProp.setPrice(new Money(new BigDecimal("12000.00"), "INR"));
                luxuryProp.setSecurityDeposit(new Money(new BigDecimal("30000.00"), "INR"));
                luxuryProp.setMaintenanceCharges(new Money(new BigDecimal("2000.00"), "INR"));
                luxuryProp.setNegotiable(true);
                luxuryProp.setBedrooms(2);
                luxuryProp.setBathrooms(2);
                luxuryProp.setParkingCount(1);
                luxuryProp.setParkingType("COVERED");
                luxuryProp.setFurnishingStatus(FurnishingStatus.FULLY_FURNISHED);
                luxuryProp.setPetFriendly(true);
                luxuryProp.setAddress(new Address("Flat 402, Sea Breeze", "Bandra West", "Mumbai", "Maharashtra", "India", "400050"));
                luxuryProp.setGeoLocation(new GeoLocation(new BigDecimal("19.0760"), new BigDecimal("72.8777")));
                luxuryProp.setArea(new AreaMeasurement(new BigDecimal("1200.00"), AreaUnit.SQ_FT));
                luxuryProp.setPublishedAt(Instant.now());
                luxuryProp.setSlug("luxury-apartment-in-bandra");
                
                Set<String> amenities = new HashSet<>();
                amenities.add("Gym");
                amenities.add("Lift");
                amenities.add("Balcony");
                amenities.add("Security");
                luxuryProp.setAmenities(amenities);
                luxuryProp.generateListingRef("Mumbai");
                
                luxuryProp = propertyRepository.save(luxuryProp);
                log.info("Seeded property: " + luxuryProp.getTitle());
                
                // Index it
                searchIndexService.indexProperty(luxuryProp.getId());
                log.info("Indexed property: " + luxuryProp.getTitle());

                // Property 2: Cozy Flat in Bandra
                Property cozyProp = new Property();
                cozyProp.setTitle("Cozy Flat near Station");
                cozyProp.setDescription("Nice cozy 1 BHK flat near Bandra railway station, perfect for students or bachelors.");
                cozyProp.setOwnerId(owner.getId());
                cozyProp.setPropertyType(PropertyType.FLAT);
                cozyProp.setListingPurpose(ListingPurpose.RENT);
                cozyProp.setStatus(PropertyStatus.ACTIVE);
                cozyProp.setVisibility(PropertyVisibility.PUBLIC);
                cozyProp.setPrice(new Money(new BigDecimal("8000.00"), "INR"));
                cozyProp.setSecurityDeposit(new Money(new BigDecimal("15000.00"), "INR"));
                cozyProp.setMaintenanceCharges(new Money(new BigDecimal("1000.00"), "INR"));
                cozyProp.setNegotiable(false);
                cozyProp.setBedrooms(1);
                cozyProp.setBathrooms(1);
                cozyProp.setParkingCount(0);
                cozyProp.setFurnishingStatus(FurnishingStatus.SEMI_FURNISHED);
                cozyProp.setPetFriendly(false);
                cozyProp.setAddress(new Address("Room 12, Station Road", "Bandra West", "Mumbai", "Maharashtra", "India", "400050"));
                cozyProp.setGeoLocation(new GeoLocation(new BigDecimal("19.0820"), new BigDecimal("72.8820")));
                cozyProp.setArea(new AreaMeasurement(new BigDecimal("600.00"), AreaUnit.SQ_FT));
                cozyProp.setPublishedAt(Instant.now());
                cozyProp.setSlug("cozy-flat-near-station");
                
                Set<String> cozyAmenities = new HashSet<>();
                cozyAmenities.add("Security");
                cozyAmenities.add("Water Supply");
                cozyProp.setAmenities(cozyAmenities);
                cozyProp.generateListingRef("Mumbai");
                
                cozyProp = propertyRepository.save(cozyProp);
                log.info("Seeded property: " + cozyProp.getTitle());
                
                // Index it
                searchIndexService.indexProperty(cozyProp.getId());
                log.info("Indexed property: " + cozyProp.getTitle());
            }

            log.info("Development data seeding completed successfully.");
        } catch (Exception e) {
            log.error("Failed to seed development data: {}", e.getMessage(), e);
        }
    }
}
