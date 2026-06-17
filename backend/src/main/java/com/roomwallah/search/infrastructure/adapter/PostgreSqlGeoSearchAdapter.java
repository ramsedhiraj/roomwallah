package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.GeoSearchPort;
import org.springframework.stereotype.Component;

@Component("postgreSqlGeoSearchAdapter")
public class PostgreSqlGeoSearchAdapter implements GeoSearchPort {

    @Override
    public String buildDistanceCondition(String latCol, String lonCol, double centerLat, double centerLon, double radiusKm) {
        // radiusKm * 1000 to convert to meters for ST_DWithin
        return String.format("ST_DWithin(ST_SetSRID(ST_MakePoint(%s, %s), 4326)::geography, ST_SetSRID(ST_MakePoint(%f, %f), 4326)::geography, %f)",
                lonCol, latCol, centerLon, centerLat, radiusKm * 1000);
    }

    @Override
    public String buildDistanceSelect(String latCol, String lonCol, double centerLat, double centerLon) {
        // Returns distance in kilometers
        return String.format("ST_Distance(ST_SetSRID(ST_MakePoint(%s, %s), 4326)::geography, ST_SetSRID(ST_MakePoint(%f, %f), 4326)::geography) / 1000.0",
                lonCol, latCol, centerLon, centerLat);
    }

    @Override
    public boolean isPostGisAvailable() {
        return true;
    }
}
