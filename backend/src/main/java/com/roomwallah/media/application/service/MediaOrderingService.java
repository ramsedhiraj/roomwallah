package com.roomwallah.media.application.service;

import java.util.List;
import java.util.UUID;

public interface MediaOrderingService {
    void reorder(UUID propertyId, UUID ownerId, List<UUID> mediaIds);
    void reposition(UUID propertyId, UUID ownerId, UUID mediaId, UUID prevMediaId, UUID nextMediaId);
}
