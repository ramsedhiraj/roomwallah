package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SearchResponseDto {
    List<PropertyCardDto> results;
    String nextCursor;
    long totalCount;
    long executionTimeMs;
}
