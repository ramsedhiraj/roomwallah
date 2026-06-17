package com.roomwallah.search.domain.model;

import lombok.Value;
import java.math.BigDecimal;

@Value
public class PriceRange {
    BigDecimal minPrice;
    BigDecimal maxPrice;
}
