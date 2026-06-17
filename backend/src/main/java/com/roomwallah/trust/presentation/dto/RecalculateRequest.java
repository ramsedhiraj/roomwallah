package com.roomwallah.trust.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class RecalculateRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
}
