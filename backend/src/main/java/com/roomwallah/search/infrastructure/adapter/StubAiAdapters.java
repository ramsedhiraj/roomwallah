package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.entity.SearchDocument;
import com.roomwallah.search.domain.port.EmbeddingProviderPort;
import com.roomwallah.search.domain.port.LLMQueryExpansionPort;
import com.roomwallah.search.domain.port.RerankingPort;
import com.roomwallah.search.domain.port.VectorSearchPort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

public class StubAiAdapters {

    @Component
    public static class StubVectorSearchAdapter implements VectorSearchPort {
        @Override
        public List<SearchDocument> findSimilar(float[] embedding, int limit) {
            return Collections.emptyList();
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }

    @Component
    public static class StubEmbeddingProviderAdapter implements EmbeddingProviderPort {
        @Override
        public float[] embed(String text) {
            return new float[0];
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }

    @Component
    public static class StubRerankingAdapter implements RerankingPort {
        @Override
        public List<SearchDocument> rerank(String query, List<SearchDocument> candidates) {
            return candidates;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }

    @Component
    public static class StubLLMQueryExpansionAdapter implements LLMQueryExpansionPort {
        @Override
        public String expandQuery(String originalQuery) {
            return originalQuery;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}
