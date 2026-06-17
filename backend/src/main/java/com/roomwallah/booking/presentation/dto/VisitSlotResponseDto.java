package com.roomwallah.booking.presentation.dto;

import com.roomwallah.booking.domain.entity.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitSlotResponseDto {
    private UUID id;
    private UUID propertyId;
    private Instant startTime;
    private Instant endTime;
    private int maxBookings;
    private int currentBookings;
    private SlotStatus status;
}
