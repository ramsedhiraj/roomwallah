package com.roomwallah.search.application.service;

import java.time.Instant;
import java.util.UUID;

public interface SearchIndexService {
    void indexProperty(UUID propertyId);
    void reindexProperty(UUID propertyId);
    void removeProperty(UUID propertyId);
    void updateOwnerTrustScore(UUID ownerId, int trustScore);
    void updateOwnerBadge(UUID ownerId, String badge);
    void triggerFullReindexing();
    void triggerIncrementalReindexing(Instant since);
    void reconcileDrift();
}
