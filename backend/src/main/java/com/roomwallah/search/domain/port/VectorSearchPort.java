package com.roomwallah.search.domain.port;

import com.roomwallah.search.domain.entity.SearchDocument;

import java.util.List;

public interface VectorSearchPort {

    List<SearchDocument> findSimilar(float[] embedding, int limit);

    boolean isAvailable();
}
