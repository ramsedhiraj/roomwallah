package com.roomwallah.property.application.facade;

import com.roomwallah.property.presentation.dto.CreatePropertyRequest;
import com.roomwallah.property.presentation.dto.PropertyResponse;
import com.roomwallah.property.presentation.dto.UpdatePropertyRequest;

import java.util.List;
import java.util.UUID;

public interface PropertyFacade {
    PropertyResponse createDraft(CreatePropertyRequest request);
    PropertyResponse updateProperty(UUID propertyId, UpdatePropertyRequest request);
    PropertyResponse getPropertyById(UUID propertyId);
    List<PropertyResponse> getMyProperties();
    PropertyResponse submitForVerification(UUID propertyId);
    PropertyResponse approveAndPublish(UUID propertyId);
    PropertyResponse pauseListing(UUID propertyId);
    PropertyResponse archiveListing(UUID propertyId);
    void deleteProperty(UUID propertyId);
}
