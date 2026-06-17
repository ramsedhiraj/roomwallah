package com.roomwallah.recommendation.repository;

import com.roomwallah.recommendation.domain.RecommendationClick;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RecommendationClickRepository extends JpaRepository<RecommendationClick, UUID> {
}
