package com.roomwallah.booking.presentation.dto;

import com.roomwallah.booking.domain.entity.LeadStatus;
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
public class LeadResponseDto {
    private UUID id;
    private UUID propertyId;
    private UUID tenantId;
    private UUID ownerId;
    private LeadStatus status;
    private String inquiryText;
    private String contactPhone;
    private String contactEmail;
    private int leadScore;
    private String leadScoreExplanation;
    private Instant createdAt;
}
