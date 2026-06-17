package com.roomwallah.media.application.service;

import java.util.UUID;

public interface CoverImageService {
    void setCoverImage(UUID propertyId, UUID ownerId, UUID mediaId);
}
