package com.roomwallah.featurestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MlFeatureRepository extends JpaRepository<MlFeature, String> {
}
