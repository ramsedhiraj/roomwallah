package com.roomwallah.search.domain.model;

import lombok.Value;

@Value
public class SortOption {
    String field;
    boolean ascending;
}
