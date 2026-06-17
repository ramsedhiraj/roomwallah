package com.roomwallah.search.domain.model;

import lombok.Value;

@Value
public class GeoRadius {
    Double latitude;
    Double longitude;
    Double radiusKm;
}
