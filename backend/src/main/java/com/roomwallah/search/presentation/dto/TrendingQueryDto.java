package com.roomwallah.search.presentation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TrendingQueryDto {
    String queryText;
    String city;
    int searchCount;
}
