package com.roomwallah.media.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {
    @NotNull
    private UUID propertyId;

    @NotEmpty
    private List<UUID> mediaIds;
}
