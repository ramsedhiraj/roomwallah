package com.roomwallah.booking.presentation.dto;

import com.roomwallah.booking.domain.entity.VisitStatus;
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
public class PropertyVisitResponseDto {
    private UUID id;
    private UUID propertyId;
    private UUID tenantId;
    private UUID visitSlotId;
    private VisitStatus status;
    private Instant startTime;
    private Instant endTime;
    private String notes;
    private Instant createdAt;
}
