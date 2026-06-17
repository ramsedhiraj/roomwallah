package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class SavedSearchResponseDto {
    UUID id;
    String serializedQuery;
    boolean notificationEnabled;
    Instant lastTriggeredAt;
    Instant createdAt;
}
