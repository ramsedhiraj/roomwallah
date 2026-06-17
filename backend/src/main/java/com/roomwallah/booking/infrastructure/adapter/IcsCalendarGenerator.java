package com.roomwallah.booking.infrastructure.adapter;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.data.CalendarOutputter;

public final class IcsCalendarGenerator {

    private IcsCalendarGenerator() {
        // Prevent instantiation
    }

    /**
     * Generates a valid RFC 5545 iCalendar string (.ics) for a booking or visit event using ical4j.
     */
    public static String generateIcs(UUID eventId, String summary, String description, Instant start, Instant end) {
        if (eventId == null || summary == null || start == null || end == null) {
            throw new IllegalArgumentException("EventId, summary, start, and end times must not be null");
        }

        try {
            Calendar calendar = new Calendar()
                    .withProdId("-//RoomWallah//Booking System//EN")
                    .withDefaults()
                    .getFluentTarget();
            calendar.add(ImmutableVersion.VERSION_2_0);
            calendar.add(ImmutableCalScale.GREGORIAN);

            ZonedDateTime startZoned = start.atZone(ZoneId.of("UTC"));
            ZonedDateTime endZoned = end.atZone(ZoneId.of("UTC"));

            VEvent event = new VEvent(startZoned, endZoned, summary);
            event.add(new Uid(eventId.toString() + "@roomwallah.com"));

            if (description != null && !description.isBlank()) {
                event.add(new Description(description));
            }

            calendar.add(event);

            StringWriter writer = new StringWriter();
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, writer);
            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate standards-compliant iCalendar using ical4j", e);
        }
    }
}
