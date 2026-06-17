package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RecommendationResponseDto {
    PropertyCardDto property;
    List<String> reasons;
}
