package com.roomwallah.search.domain.event;

import lombok.Value;
import java.util.UUID;

@Value
public class SearchExecutedEvent {
    String queryText;
    String city;
    UUID userId;
    int resultCount;
    long executionTimeMs;
}
