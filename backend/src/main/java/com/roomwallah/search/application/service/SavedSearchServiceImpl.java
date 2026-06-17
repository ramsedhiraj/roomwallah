package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.entity.SavedSearch;
import com.roomwallah.search.domain.repository.SavedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedSearchServiceImpl implements SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;

    private static final int MAX_SAVED_SEARCHES = 50;

    @Override
    @Transactional
    public SavedSearch create(UUID userId, String serializedQuery, boolean notificationEnabled) {
        long count = savedSearchRepository.countByUserId(userId);
        if (count >= MAX_SAVED_SEARCHES) {
            throw new IllegalStateException("Maximum limit of " + MAX_SAVED_SEARCHES + " saved searches reached.");
        }

        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setId(UUID.randomUUID());
        savedSearch.setUserId(userId);
        savedSearch.setSerializedQuery(serializedQuery);
        savedSearch.setNotificationEnabled(notificationEnabled);
        savedSearch.setCreatedAt(Instant.now());
        savedSearch.setUpdatedAt(Instant.now());

        return savedSearchRepository.save(savedSearch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearch> getByUser(UUID userId) {
        return savedSearchRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Saved search not found."));

        if (!savedSearch.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized action: You do not own this saved search.");
        }

        savedSearchRepository.delete(savedSearch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearch> getEnabledForNotifications() {
        return savedSearchRepository.findByNotificationEnabledTrue();
    }

    @Override
    @Transactional
    public void updateLastTriggered(UUID id) {
        savedSearchRepository.findById(id).ifPresent(ss -> {
            ss.setLastTriggeredAt(Instant.now());
            ss.setUpdatedAt(Instant.now());
            savedSearchRepository.save(ss);
        });
    }
}
