package com.roomwallah.common.ai.impl;

import com.roomwallah.common.ai.AiProvider;
import org.springframework.stereotype.Component;

@Component
public class DefaultAiProvider implements AiProvider {
    @Override
    public String generate(String prompt) {
        if (prompt.contains("intent")) {
            return "{\"intent\": \"SEARCH\", \"city\": \"Mumbai\", \"budget\": 50000}";
        }
        return "Generated response for: " + prompt;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getModelIdentifier() {
        return "gpt-4o-mini";
    }
}
