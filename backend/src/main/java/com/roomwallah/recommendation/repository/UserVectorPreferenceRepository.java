package com.roomwallah.recommendation.repository;

import com.roomwallah.recommendation.domain.UserVectorPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserVectorPreferenceRepository extends JpaRepository<UserVectorPreference, UUID> {
    Optional<UserVectorPreference> findByUserId(UUID userId);
}
