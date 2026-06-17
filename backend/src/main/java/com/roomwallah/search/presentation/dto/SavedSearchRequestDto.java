package com.roomwallah.search.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SavedSearchRequestDto {
    @NotBlank
    private String serializedQuery;
    private boolean notificationEnabled;
}
