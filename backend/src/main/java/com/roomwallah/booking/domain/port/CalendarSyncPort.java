package com.roomwallah.booking.domain.port;

import java.time.Instant;
import java.util.UUID;

public interface CalendarSyncPort {
    boolean exportEvent(UUID entityId, String title, Instant start, Instant end);
}
