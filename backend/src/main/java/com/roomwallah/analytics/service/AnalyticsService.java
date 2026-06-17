package com.roomwallah.analytics.service;

import com.roomwallah.analytics.domain.DailyMetricsSnapshot;
import com.roomwallah.analytics.domain.GeographicAnalytics;
import com.roomwallah.analytics.domain.HourlyMetricsSnapshot;
import com.roomwallah.analytics.repository.DailyMetricsSnapshotRepository;
import com.roomwallah.analytics.repository.GeographicAnalyticsRepository;
import com.roomwallah.analytics.repository.HourlyMetricsSnapshotRepository;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final DailyMetricsSnapshotRepository dailyMetricsSnapshotRepository;
    private final GeographicAnalyticsRepository geographicAnalyticsRepository;
    private final HourlyMetricsSnapshotRepository hourlyMetricsSnapshotRepository;

    @Transactional
    public void performHourlyAggregation(LocalDate date, int hour) {
        log.info("Starting hourly aggregation for date: {}, hour: {}", date, hour);

        Instant start = date.atStartOfDay(ZoneOffset.UTC).plusSeconds(hour * 3600L).toInstant();
        Instant end = start.plusSeconds(3600L);

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt().isAfter(start) && b.getCreatedAt().isBefore(end))
                .toList();

        int totalBookings = bookings.size();

        long activeListings = propertyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .count();

        BigDecimal revenue = bookings.stream()
                .map(Booking::getPriceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal occupancyRate = BigDecimal.ZERO;
        if (activeListings > 0) {
            occupancyRate = BigDecimal.valueOf(totalBookings * 100.0 / activeListings);
        }

        HourlyMetricsSnapshot snapshot = hourlyMetricsSnapshotRepository
                .findBySnapshotDateAndSnapshotHour(date, hour)
                .orElseGet(() -> HourlyMetricsSnapshot.builder().snapshotDate(date).snapshotHour(hour).build());

        snapshot.setTotalBookings(totalBookings);
        snapshot.setActiveListings((int) activeListings);
        snapshot.setRevenue(revenue);
        snapshot.setOccupancyRate(occupancyRate);

        hourlyMetricsSnapshotRepository.save(snapshot);
        log.info("Finished hourly aggregation for date: {}, hour: {}", date, hour);
    }

    @Transactional
    public void performDailyAggregation(LocalDate date) {
        log.info("Starting daily aggregation for date: {}", date);

        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt().isAfter(start) && b.getCreatedAt().isBefore(end))
                .toList();

        int totalBookings = bookings.size();

        long activeListings = propertyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .count();

        BigDecimal revenue = bookings.stream()
                .map(Booking::getPriceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal occupancyRate = BigDecimal.ZERO;
        if (activeListings > 0) {
            occupancyRate = BigDecimal.valueOf(totalBookings * 100.0 / activeListings);
        }

        // Rolling 7d stats (including this day)
        int rolling7dBookings = totalBookings;
        BigDecimal rolling7dRevenue = revenue;
        for (int i = 1; i < 7; i++) {
            Optional<DailyMetricsSnapshot> prevOpt = dailyMetricsSnapshotRepository.findBySnapshotDate(date.minusDays(i));
            if (prevOpt.isPresent()) {
                rolling7dBookings += prevOpt.get().getTotalBookings();
                rolling7dRevenue = rolling7dRevenue.add(prevOpt.get().getRevenue());
            }
        }

        // Rolling 30d stats (including this day)
        int rolling30dBookings = totalBookings;
        BigDecimal rolling30dRevenue = revenue;
        for (int i = 1; i < 30; i++) {
            Optional<DailyMetricsSnapshot> prevOpt = dailyMetricsSnapshotRepository.findBySnapshotDate(date.minusDays(i));
            if (prevOpt.isPresent()) {
                rolling30dBookings += prevOpt.get().getTotalBookings();
                rolling30dRevenue = rolling30dRevenue.add(prevOpt.get().getRevenue());
            }
        }

        DailyMetricsSnapshot snapshot = dailyMetricsSnapshotRepository.findBySnapshotDate(date)
                .orElseGet(() -> DailyMetricsSnapshot.builder().snapshotDate(date).build());

        snapshot.setTotalBookings(totalBookings);
        snapshot.setActiveListings((int) activeListings);
        snapshot.setRevenue(revenue);
        snapshot.setOccupancyRate(occupancyRate);
        snapshot.setRolling7dBookings(rolling7dBookings);
        snapshot.setRolling7dRevenue(rolling7dRevenue);
        snapshot.setRolling30dBookings(rolling30dBookings);
        snapshot.setRolling30dRevenue(rolling30dRevenue);

        dailyMetricsSnapshotRepository.save(snapshot);

        // Geographic stats
        Map<String, Map<String, List<Booking>>> geoGroups = new HashMap<>();
        for (Booking b : bookings) {
            Property p = propertyRepository.findById(b.getPropertyId()).orElse(null);
            if (p != null && p.getAddress() != null) {
                String city = p.getAddress().getCity();
                String country = p.getAddress().getCountry();
                if (city != null && country != null) {
                    geoGroups.computeIfAbsent(country, k -> new HashMap<>())
                            .computeIfAbsent(city, k -> new ArrayList<>())
                            .add(b);
                }
            }
        }

        for (Map.Entry<String, Map<String, List<Booking>>> countryEntry : geoGroups.entrySet()) {
            String country = countryEntry.getKey();
            for (Map.Entry<String, List<Booking>> cityEntry : countryEntry.getValue().entrySet()) {
                String city = cityEntry.getKey();
                List<Booking> cityBookings = cityEntry.getValue();

                int bookingCount = cityBookings.size();
                BigDecimal geoRevenue = cityBookings.stream()
                        .map(Booking::getPriceAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                GeographicAnalytics geoStat = geographicAnalyticsRepository
                        .findBySnapshotDateAndCityAndCountry(date, city, country)
                        .orElseGet(() -> GeographicAnalytics.builder()
                                .snapshotDate(date)
                                .city(city)
                                .country(country)
                                .build());
                geoStat.setBookingCount(bookingCount);
                geoStat.setRevenueGenerated(geoRevenue);
                geographicAnalyticsRepository.save(geoStat);
            }
        }

        log.info("Finished daily aggregation for date: {}", date);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatsFiltered(
            LocalDate date,
            String state,
            String city,
            String category,
            UUID ownerId,
            String userSegment
    ) {
        List<Property> properties = propertyRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> state == null || (p.getAddress() != null && state.equalsIgnoreCase(p.getAddress().getState())))
                .filter(p -> city == null || (p.getAddress() != null && city.equalsIgnoreCase(p.getAddress().getCity())))
                .filter(p -> category == null || (p.getPropertyType() != null && category.equalsIgnoreCase(p.getPropertyType().name())))
                .filter(p -> ownerId == null || ownerId.equals(p.getOwnerId()))
                .toList();

        Set<UUID> propertyIds = new HashSet<>();
        for (Property p : properties) {
            propertyIds.add(p.getId());
        }

        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt().isAfter(start) && b.getCreatedAt().isBefore(end))
                .filter(b -> propertyIds.contains(b.getPropertyId()))
                .filter(b -> ownerId == null || ownerId.equals(b.getOwnerId()))
                .toList();

        int totalBookings = bookings.size();
        int activeListings = (int) properties.stream().filter(p -> p.getStatus() == PropertyStatus.ACTIVE).count();
        BigDecimal revenue = bookings.stream()
                .map(Booking::getPriceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal occupancyRate = BigDecimal.ZERO;
        if (activeListings > 0) {
            occupancyRate = BigDecimal.valueOf(totalBookings * 100.0 / activeListings);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("date", date);
        stats.put("totalBookings", totalBookings);
        stats.put("activeListings", activeListings);
        stats.put("revenue", revenue);
        stats.put("occupancyRate", occupancyRate);
        stats.put("filteredPropertiesCount", properties.size());

        // Include rolling stats if available
        Optional<DailyMetricsSnapshot> snapshotOpt = dailyMetricsSnapshotRepository.findBySnapshotDate(date);
        if (snapshotOpt.isPresent()) {
            DailyMetricsSnapshot snap = snapshotOpt.get();
            stats.put("rolling7dBookings", snap.getRolling7dBookings());
            stats.put("rolling7dRevenue", snap.getRolling7dRevenue());
            stats.put("rolling30dBookings", snap.getRolling30dBookings());
            stats.put("rolling30dRevenue", snap.getRolling30dRevenue());
        } else {
            stats.put("rolling7dBookings", totalBookings);
            stats.put("rolling7dRevenue", revenue);
            stats.put("rolling30dBookings", totalBookings);
            stats.put("rolling30dRevenue", revenue);
        }

        return stats;
    }
}
