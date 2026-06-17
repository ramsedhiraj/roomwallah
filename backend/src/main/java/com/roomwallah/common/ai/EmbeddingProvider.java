package com.roomwallah.common.ai;

public interface EmbeddingProvider {
    double[] embed(String text);
    boolean isAvailable();
    String getModelIdentifier();
    int getEmbeddingVersion();
}
