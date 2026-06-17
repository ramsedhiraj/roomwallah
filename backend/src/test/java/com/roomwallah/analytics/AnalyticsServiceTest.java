package com.roomwallah.analytics;

import com.roomwallah.analytics.domain.DailyMetricsSnapshot;
import com.roomwallah.analytics.domain.GeographicAnalytics;
import com.roomwallah.analytics.domain.HourlyMetricsSnapshot;
import com.roomwallah.analytics.repository.DailyMetricsSnapshotRepository;
import com.roomwallah.analytics.repository.GeographicAnalyticsRepository;
import com.roomwallah.analytics.repository.HourlyMetricsSnapshotRepository;
import com.roomwallah.analytics.service.AnalyticsService;
import com.roomwallah.booking.domain.entity.Booking;
import com.roomwallah.booking.domain.repository.BookingRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class AnalyticsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private DailyMetricsSnapshotRepository dailyMetricsSnapshotRepository;

    @Mock
    private GeographicAnalyticsRepository geographicAnalyticsRepository;

    @Mock
    private HourlyMetricsSnapshotRepository hourlyMetricsSnapshotRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsService(
                bookingRepository, propertyRepository, 
                dailyMetricsSnapshotRepository, geographicAnalyticsRepository, 
                hourlyMetricsSnapshotRepository
        );
    }

    @Test
    public void testPerformHourlyAggregation() {
        LocalDate date = LocalDate.now();
        int hour = 14;

        // Mock bookings created during that hour
        Booking b = new Booking();
        b.setPriceAmount(BigDecimal.valueOf(2500.00));
        Instant bookingTime = date.atStartOfDay(ZoneOffset.UTC).plusSeconds(hour * 3600L + 1800).toInstant();
        b.setCreatedAt(bookingTime);

        when(bookingRepository.findAll()).thenReturn(List.of(b));
        when(propertyRepository.findAll()).thenReturn(Collections.emptyList());
        when(hourlyMetricsSnapshotRepository.findBySnapshotDateAndSnapshotHour(date, hour))
                .thenReturn(Optional.empty());

        analyticsService.performHourlyAggregation(date, hour);

        verify(hourlyMetricsSnapshotRepository, times(1)).save(any(HourlyMetricsSnapshot.class));
    }

    @Test
    public void testPerformDailyAggregation() {
        LocalDate date = LocalDate.now();

        Booking b = new Booking();
        b.setPriceAmount(BigDecimal.valueOf(5000.00));
        b.setCreatedAt(date.atStartOfDay(ZoneOffset.UTC).plusSeconds(3600).toInstant());

        Property p = new Property();
        p.setStatus(PropertyStatus.ACTIVE);

        when(bookingRepository.findAll()).thenReturn(List.of(b));
        when(propertyRepository.findAll()).thenReturn(List.of(p));
        when(dailyMetricsSnapshotRepository.findBySnapshotDate(date)).thenReturn(Optional.empty());

        analyticsService.performDailyAggregation(date);

        verify(dailyMetricsSnapshotRepository, times(1)).save(any(DailyMetricsSnapshot.class));
    }
}
