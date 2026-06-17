package com.roomwallah.media.application.service;

import java.util.UUID;

public interface MediaDeletionService {
    void delete(UUID mediaId, UUID ownerId);
}
