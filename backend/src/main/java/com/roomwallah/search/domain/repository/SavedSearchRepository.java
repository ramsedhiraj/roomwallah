package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {

    List<SavedSearch> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    List<SavedSearch> findByNotificationEnabledTrue();
}
