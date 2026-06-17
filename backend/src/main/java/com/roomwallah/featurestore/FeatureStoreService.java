package com.roomwallah.featurestore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureStoreService {

    private final MlFeatureRepository featureRepository;

    @Transactional
    public void saveFeature(String key, String value, int version) {
        log.info("Saving ML feature: {} = {}, version: {}", key, value, version);
        MlFeature feature = MlFeature.builder()
                .featureKey(key)
                .featureValue(value)
                .featureVersion(version)
                .lastUpdatedAt(Instant.now())
                .build();
        featureRepository.save(feature);
    }

    public Optional<MlFeature> getFeature(String key) {
        return featureRepository.findById(key);
    }

    public boolean isFeatureFresh(String key, Duration maxAge) {
        Optional<MlFeature> featOpt = getFeature(key);
        if (featOpt.isEmpty()) {
            return false;
        }
        Instant lastUpdated = featOpt.get().getLastUpdatedAt();
        return Duration.between(lastUpdated, Instant.now()).compareTo(maxAge) <= 0;
    }

    public String getFeatureAtTimestamp(String key, Instant timestamp) {
        return getFeature(key).map(MlFeature::getFeatureValue).orElse(null);
    }
}
