package com.roomwallah.recommendation.service;

import com.roomwallah.recommendation.domain.ListingVector;
import com.roomwallah.recommendation.domain.VectorStore;
import com.roomwallah.recommendation.repository.ListingVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseVectorStore implements VectorStore {

    private final ListingVectorRepository listingVectorRepository;

    @Override
    @Transactional
    public void saveEmbedding(UUID listingId, double[] embedding, int version, String modelIdentifier) {
        Optional<ListingVector> existing = listingVectorRepository.findByListingId(listingId);
        ListingVector vector;
        if (existing.isPresent()) {
            vector = existing.get();
            vector.setEmbedding(embedding);
            vector.setEmbeddingVersion(version);
            vector.setModelIdentifier(modelIdentifier);
            vector.setGenerationTimestamp(Instant.now());
            vector.setUpdatedAt(Instant.now());
        } else {
            vector = ListingVector.builder()
                    .listingId(listingId)
                    .embedding(embedding)
                    .embeddingVersion(version)
                    .modelIdentifier(modelIdentifier)
                    .generationTimestamp(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
        listingVectorRepository.save(vector);
        log.info("Saved vector embedding for listing: {}, version: {}", listingId, version);
    }

    @Override
    @Transactional(readOnly = true)
    public double[] getEmbedding(UUID listingId) {
        return listingVectorRepository.findByListingId(listingId)
                .map(ListingVector::getEmbedding)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findSimilarListings(double[] queryEmbedding, int limit) {
        List<ListingVector> allVectors = listingVectorRepository.findAll();
        
        class SimilarItem implements Comparable<SimilarItem> {
            final UUID listingId;
            final double similarity;

            SimilarItem(UUID listingId, double similarity) {
                this.listingId = listingId;
                this.similarity = similarity;
            }

            @Override
            public int compareTo(SimilarItem o) {
                return Double.compare(o.similarity, this.similarity);
            }
        }

        List<SimilarItem> list = new ArrayList<>();
        for (ListingVector lv : allVectors) {
            double sim = cosineSimilarity(queryEmbedding, lv.getEmbedding());
            list.add(new SimilarItem(lv.getListingId(), sim));
        }

        Collections.sort(list);
        List<UUID> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, list.size()); i++) {
            results.add(list.get(i).listingId);
        }
        return results;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
