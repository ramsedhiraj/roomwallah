package com.roomwallah.property.application.service;

import com.roomwallah.common.ai.EmbeddingProvider;
import com.roomwallah.property.domain.entity.Property;
import com.roomwallah.property.domain.entity.PropertyStatus;
import com.roomwallah.property.domain.repository.PropertyRepository;
import com.roomwallah.recommendation.domain.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineEmbeddingPipeline {

    private final PropertyRepository propertyRepository;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;

    // Run every hour to compute missing or dirty embeddings
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void runEmbeddingPipeline() {
        log.info("Starting offline listing vector embedding generation pipeline...");
        List<Property> activeProperties = propertyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE && !p.isDeleted())
                .toList();

        int updatedCount = 0;
        for (Property property : activeProperties) {
            double[] existingVec = vectorStore.getEmbedding(property.getId());
            if (existingVec == null) {
                log.info("Generating vector embedding for listing: {}", property.getId());
                try {
                    String contentToEmbed = String.format("%s. %s. Located in %s, %s. Amenities: %s",
                            property.getTitle(),
                            property.getDescription() != null ? property.getDescription() : "",
                            property.getAddress() != null ? property.getAddress().getLine2() : "",
                            property.getAddress() != null ? property.getAddress().getCity() : "",
                            property.getAmenities() != null ? String.join(", ", property.getAmenities()) : ""
                    );
                    
                    double[] embedding = embeddingProvider.embed(contentToEmbed);
                    vectorStore.saveEmbedding(
                            property.getId(),
                            embedding,
                            embeddingProvider.getEmbeddingVersion(),
                            embeddingProvider.getModelIdentifier()
                    );
                    updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to generate embedding for property: {}", property.getId(), e);
                }
            }
        }
        log.info("Offline listing vector embedding generation pipeline completed. Updated {} listings.", updatedCount);
    }
}
