package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.presentation.dto.UpdatePropertyRequest;
import com.roomwallah.user.entity.User;

import java.util.UUID;

public interface PropertyUpdateService {
    Property updateProperty(User owner, UUID propertyId, UpdatePropertyRequest request);
}
