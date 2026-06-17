package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.GeoSearchPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class DelegatingGeoSearchAdapter implements GeoSearchPort {

    private final JdbcTemplate jdbcTemplate;
    private final PostgreSqlGeoSearchAdapter postgreSqlGeoSearchAdapter;
    private final HaversineGeoSearchAdapter haversineGeoSearchAdapter;
    private boolean usePostGis = false;

    @PostConstruct
    public void checkPostGis() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'postgis'", Integer.class);
            usePostGis = count != null && count > 0;
            log.info("Geospatial search config: PostGIS extension detected = {}", usePostGis);
        } catch (Exception e) {
            log.warn("Failed to check PostGIS extension, defaulting to Haversine fallback. Error: {}", e.getMessage());
            usePostGis = false;
        }
    }

    private GeoSearchPort getActiveAdapter() {
        return usePostGis ? postgreSqlGeoSearchAdapter : haversineGeoSearchAdapter;
    }

    @Override
    public String buildDistanceCondition(String latCol, String lonCol, double centerLat, double centerLon, double radiusKm) {
        return getActiveAdapter().buildDistanceCondition(latCol, lonCol, centerLat, centerLon, radiusKm);
    }

    @Override
    public String buildDistanceSelect(String latCol, String lonCol, double centerLat, double centerLon) {
        return getActiveAdapter().buildDistanceSelect(latCol, lonCol, centerLat, centerLon);
    }

    @Override
    public boolean isPostGisAvailable() {
        return usePostGis;
    }
}
