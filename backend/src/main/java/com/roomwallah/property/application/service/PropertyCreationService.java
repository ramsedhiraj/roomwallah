package com.roomwallah.property.application.service;

import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.user.entity.User;

public interface PropertyCreationService {
    Property createDraft(User owner, CreatePropertyRequest request);
}
