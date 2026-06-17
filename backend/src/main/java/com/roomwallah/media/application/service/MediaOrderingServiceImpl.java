package com.roomwallah.media.application.service;

import com.roomwallah.exception.ResourceNotFoundException;
import com.roomwallah.media.domain.entity.PropertyMedia;
import com.roomwallah.media.domain.repository.PropertyMediaRepository;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaOrderingServiceImpl implements MediaOrderingService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;

    @Override
    @Transactional
    public void reorder(UUID propertyId, UUID ownerId, List<UUID> mediaIds) {
        // 1. Verify Property ownership
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // 2. Fetch all media
        List<PropertyMedia> mediaList = propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId);
        Map<UUID, PropertyMedia> mediaMap = mediaList.stream()
                .collect(Collectors.toMap(PropertyMedia::getId, Function.identity()));

        // 3. Assign gap-based order values (1000, 2000, ...)
        for (int i = 0; i < mediaIds.size(); i++) {
            UUID mediaId = mediaIds.get(i);
            PropertyMedia media = mediaMap.get(mediaId);
            if (media == null) {
                throw new IllegalArgumentException("Media item does not belong to property");
            }
            media.setDisplayOrder((long) i);
        }

        propertyMediaRepository.saveAll(mediaList);
    }

    @Override
    @Transactional
    public void reposition(UUID propertyId, UUID ownerId, UUID mediaId, UUID prevMediaId, UUID nextMediaId) {
        // 1. Verify Property ownership
        Property property = propertyRepository.findById(propertyId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + propertyId));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("User does not own this property");
        }

        // 2. Load target media
        PropertyMedia target = propertyMediaRepository.findByIdAndDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        if (!target.getPropertyId().equals(propertyId)) {
            throw new IllegalArgumentException("Media item does not belong to property");
        }

        // 3. Calculate display order midpoint
        long prevOrder = 0;
        long nextOrder = 0;
        boolean triggerRebalance = false;

        if (prevMediaId != null) {
            PropertyMedia prev = propertyMediaRepository.findByIdAndDeletedFalse(prevMediaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Previous media not found: " + prevMediaId));
            if (!prev.getPropertyId().equals(propertyId)) {
                throw new IllegalArgumentException("Previous media item does not belong to property");
            }
            prevOrder = prev.getDisplayOrder();
        }

        if (nextMediaId != null) {
            PropertyMedia next = propertyMediaRepository.findByIdAndDeletedFalse(nextMediaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Next media not found: " + nextMediaId));
            if (!next.getPropertyId().equals(propertyId)) {
                throw new IllegalArgumentException("Next media item does not belong to property");
            }
            nextOrder = next.getDisplayOrder();
        }

        long newOrder;
        if (prevMediaId == null && nextMediaId == null) {
            newOrder = 1000L;
        } else if (prevMediaId == null) {
            newOrder = nextOrder / 2;
            if (newOrder <= 1) {
                triggerRebalance = true;
            }
        } else if (nextMediaId == null) {
            newOrder = prevOrder + 1000L;
        } else {
            newOrder = (prevOrder + nextOrder) / 2;
            if (nextOrder - prevOrder <= 1) {
                triggerRebalance = true;
            }
        }

        target.setDisplayOrder(newOrder);
        propertyMediaRepository.save(target);

        if (triggerRebalance) {
            log.info("Display order gap exhausted for property: {}. Scheduling background rebalance.", propertyId);
            CompletableFuture.runAsync(() -> rebalancePropertyMedia(propertyId));
        }
    }

    private void rebalancePropertyMedia(UUID propertyId) {
        try {
            log.info("Starting background rebalance of media for property: {}", propertyId);
            List<PropertyMedia> mediaList = propertyMediaRepository.findByPropertyIdAndDeletedFalseOrderByDisplayOrderAsc(propertyId);
            for (int i = 0; i < mediaList.size(); i++) {
                mediaList.get(i).setDisplayOrder((i + 1) * 1000L);
            }
            propertyMediaRepository.saveAll(mediaList);
            log.info("Successfully finished rebalance of media for property: {}", propertyId);
        } catch (Exception e) {
            log.error("Failed to rebalance media order for property " + propertyId, e);
        }
    }
}
