package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.GeoSearchPort;
import org.springframework.stereotype.Component;

@Component("haversineGeoSearchAdapter")
public class HaversineGeoSearchAdapter implements GeoSearchPort {

    @Override
    public String buildDistanceCondition(String latCol, String lonCol, double centerLat, double centerLon, double radiusKm) {
        return String.format(
            "(6371 * acos(least(1.0, greatest(-1.0, cos(radians(%f)) * cos(radians(%s)) * cos(radians(%s) - radians(%f)) + sin(radians(%f)) * sin(radians(%s)))))) <= %f",
            centerLat, latCol, lonCol, centerLon, centerLat, latCol, radiusKm
        );
    }

    @Override
    public String buildDistanceSelect(String latCol, String lonCol, double centerLat, double centerLon) {
        return String.format(
            "(6371 * acos(least(1.0, greatest(-1.0, cos(radians(%f)) * cos(radians(%s)) * cos(radians(%s) - radians(%f)) + sin(radians(%f)) * sin(radians(%s))))))",
            centerLat, latCol, lonCol, centerLon, centerLat, latCol
        );
    }

    @Override
    public boolean isPostGisAvailable() {
        return false;
    }
}
