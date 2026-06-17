package com.roomwallah.booking.infrastructure.adapter;

import com.roomwallah.booking.domain.port.CalendarSyncPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class StubGoogleCalendarAdapter implements CalendarSyncPort {

    @Override
    public boolean exportEvent(UUID entityId, String title, Instant start, Instant end) {
        log.info("Stub Google Calendar Sync: Exporting event with ID: {}, Title: '{}', Start: {}, End: {}", 
                entityId, title, start, end);
        return true;
    }
}
