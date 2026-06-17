package com.roomwallah.common.ai;

public interface AiProvider {
    String generate(String prompt);
    boolean isAvailable();
    String getModelIdentifier();
}
