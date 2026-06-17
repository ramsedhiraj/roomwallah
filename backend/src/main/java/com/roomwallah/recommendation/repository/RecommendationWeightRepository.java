package com.roomwallah.recommendation.repository;

import com.roomwallah.recommendation.domain.RecommendationWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecommendationWeightRepository extends JpaRepository<RecommendationWeight, UUID> {
}
