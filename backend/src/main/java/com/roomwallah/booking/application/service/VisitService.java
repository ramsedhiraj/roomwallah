package com.roomwallah.booking.application.service;

import com.roomwallah.booking.domain.entity.PropertyVisit;
import com.roomwallah.booking.presentation.dto.PropertyVisitRequestDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitService {
    PropertyVisit scheduleVisit(UUID tenantId, PropertyVisitRequestDto request);
    PropertyVisit recordNoShow(UUID ownerId, UUID visitId);
    PropertyVisit completeVisit(UUID ownerId, UUID visitId);
    PropertyVisit cancelVisit(UUID userId, UUID visitId);
    List<PropertyVisit> getTenantVisits(UUID tenantId);
    List<PropertyVisit> getOwnerVisits(UUID ownerId);
    Optional<PropertyVisit> getVisit(UUID id);
}
