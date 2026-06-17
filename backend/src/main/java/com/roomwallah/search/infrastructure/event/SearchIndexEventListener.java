package com.roomwallah.search.infrastructure.event;

import com.roomwallah.property.domain.event.*;
import com.roomwallah.search.application.service.SearchIndexService;
import com.roomwallah.verification.domain.event.BadgeAwardedEvent;
import com.roomwallah.verification.domain.event.TrustScoreChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchIndexEventListener {

    private final SearchIndexService searchIndexService;

    @EventListener
    public void handlePropertyCreated(PropertyCreatedEvent event) {
        log.info("Received PropertyCreatedEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.indexProperty(event.getPropertyId());
    }

    @EventListener
    public void handlePropertyUpdated(PropertyUpdatedEvent event) {
        log.info("Received PropertyUpdatedEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.reindexProperty(event.getPropertyId());
    }

    @EventListener
    public void handlePropertyPublished(PropertyPublishedEvent event) {
        log.info("Received PropertyPublishedEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.reindexProperty(event.getPropertyId());
    }

    @EventListener
    public void handlePropertyArchived(PropertyArchivedEvent event) {
        log.info("Received PropertyArchivedEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.reindexProperty(event.getPropertyId());
    }

    @EventListener
    public void handlePropertyPaused(PropertyPausedEvent event) {
        log.info("Received PropertyPausedEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.reindexProperty(event.getPropertyId());
    }

    @EventListener
    public void handlePropertySubmittedForVerification(PropertySubmittedForVerificationEvent event) {
        log.info("Received PropertySubmittedForVerificationEvent for propertyId: {}", event.getPropertyId());
        searchIndexService.reindexProperty(event.getPropertyId());
    }

    @EventListener
    public void handleTrustScoreChanged(TrustScoreChangedEvent event) {
        log.info("Received TrustScoreChangedEvent for userId: {}, new score: {}", event.userId(), event.overallScore());
        searchIndexService.updateOwnerTrustScore(event.userId(), event.overallScore());
    }

    @EventListener
    public void handleBadgeAwarded(BadgeAwardedEvent event) {
        log.info("Received BadgeAwardedEvent for userId: {}, badge: {}", event.userId(), event.badgeLevel());
        searchIndexService.updateOwnerBadge(event.userId(), event.badgeLevel() != null ? event.badgeLevel().name() : null);
    }
}
