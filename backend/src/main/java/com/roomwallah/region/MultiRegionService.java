package com.roomwallah.region;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MultiRegionService {

    private final Map<String, Boolean> regionHealth = new ConcurrentHashMap<>();
    private final Map<String, byte[]> simulatedObjectStorage = new ConcurrentHashMap<>();

    private static final String DEFAULT_REGION = "US-EAST";
    private static final String FAILOVER_REGION = "EU-WEST";

    public MultiRegionService() {
        regionHealth.put(DEFAULT_REGION, true);
        regionHealth.put(FAILOVER_REGION, true);
    }

    public String uploadFile(String key, byte[] content, String preferredRegion) {
        String region = preferredRegion;
        if (region == null || !Boolean.TRUE.equals(regionHealth.get(region))) {
            region = getHealthyRegion();
        }

        log.info("Routing upload of key: {} to region: {}", key, region);
        String storageKey = region + ":" + key;
        simulatedObjectStorage.put(storageKey, content);
        return "https://storage.roomwallah.com/" + region + "/" + key;
    }

    public byte[] downloadFile(String key) {
        String region = getHealthyRegion();
        log.info("Routing download of key: {} from active region: {}", key, region);
        String storageKey = region + ":" + key;
        
        byte[] content = simulatedObjectStorage.get(storageKey);
        if (content == null) {
            String alternateRegion = "US-EAST".equals(region) ? "EU-WEST" : "US-EAST";
            log.info("File not found in active region. Checking failover region: {}", alternateRegion);
            storageKey = alternateRegion + ":" + key;
            content = simulatedObjectStorage.get(storageKey);
        }

        if (content == null) {
            throw new IllegalArgumentException("File key not found in any region: " + key);
        }
        return content;
    }

    public void setRegionHealth(String region, boolean healthy) {
        log.info("Setting region {} health status to: {}", region, healthy);
        regionHealth.put(region, healthy);
    }

    public Map<String, Boolean> getRegionsStatus() {
        return new ConcurrentHashMap<>(regionHealth);
    }

    private String getHealthyRegion() {
        if (Boolean.TRUE.equals(regionHealth.get(DEFAULT_REGION))) {
            return DEFAULT_REGION;
        }
        if (Boolean.TRUE.equals(regionHealth.get(FAILOVER_REGION))) {
            return FAILOVER_REGION;
        }
        throw new IllegalStateException("All storage regions are currently unhealthy!");
    }
}
