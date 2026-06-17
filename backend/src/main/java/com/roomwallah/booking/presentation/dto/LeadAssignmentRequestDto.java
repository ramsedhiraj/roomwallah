package com.roomwallah.booking.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadAssignmentRequestDto {
    @NotNull(message = "Assignee ID is required")
    private UUID assigneeId;
}
