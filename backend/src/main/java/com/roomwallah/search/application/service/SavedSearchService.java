package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.entity.SavedSearch;

import java.util.List;
import java.util.UUID;

public interface SavedSearchService {
    SavedSearch create(UUID userId, String serializedQuery, boolean notificationEnabled);
    List<SavedSearch> getByUser(UUID userId);
    void delete(UUID id, UUID userId);
    List<SavedSearch> getEnabledForNotifications();
    void updateLastTriggered(UUID id);
}
