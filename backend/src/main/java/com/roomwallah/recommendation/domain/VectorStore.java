package com.roomwallah.recommendation.domain;

import java.util.List;
import java.util.UUID;

public interface VectorStore {
    void saveEmbedding(UUID listingId, double[] embedding, int version, String modelIdentifier);
    double[] getEmbedding(UUID listingId);
    List<UUID> findSimilarListings(double[] queryEmbedding, int limit);
}
