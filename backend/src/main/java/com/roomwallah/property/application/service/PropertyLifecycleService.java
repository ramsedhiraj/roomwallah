package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.user.entity.User;

import java.util.UUID;

public interface PropertyLifecycleService {
    Property pauseListing(User owner, UUID propertyId);
    Property archiveListing(User owner, UUID propertyId);
    void softDeleteProperty(User owner, UUID propertyId);
}
