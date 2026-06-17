package com.roomwallah.search.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchQuery {
    String text;
    SearchFilter filter;
    SortOption sort;
    CursorPage page;
    Boolean explain;
    String experimentalBucket;
}
