package com.roomwallah.booking.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public final class CalendarAvailabilityRule {
    private String dayOfWeek;       // e.g., "MONDAY", "TUESDAY"
    private String startTime;       // e.g., "09:00"
    private String endTime;         // e.g., "17:00"
    private boolean isAvailable;    // default true or false
}
