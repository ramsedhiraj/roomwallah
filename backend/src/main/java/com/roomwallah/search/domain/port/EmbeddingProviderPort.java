package com.roomwallah.search.domain.port;

public interface EmbeddingProviderPort {

    float[] embed(String text);

    boolean isAvailable();
}
