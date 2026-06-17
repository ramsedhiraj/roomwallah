package com.roomwallah.search.domain.port;

public interface GeoSearchPort {

    String buildDistanceCondition(String latCol, String lonCol, double centerLat, double centerLon, double radiusKm);

    String buildDistanceSelect(String latCol, String lonCol, double centerLat, double centerLon);

    boolean isPostGisAvailable();
}
