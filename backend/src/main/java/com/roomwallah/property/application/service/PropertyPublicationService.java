package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.user.entity.User;

import java.util.UUID;

public interface PropertyPublicationService {
    Property submitForVerification(User owner, UUID propertyId);
    Property approveAndPublish(UUID propertyId);
}
