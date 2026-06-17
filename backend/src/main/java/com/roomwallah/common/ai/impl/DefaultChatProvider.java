package com.roomwallah.common.ai.impl;

import com.roomwallah.common.ai.ChatProvider;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class DefaultChatProvider implements ChatProvider {
    @Override
    public String chat(List<Map<String, String>> history, String userMessage) {
        return "Hello! I am your AI property assistant. You asked: " + userMessage;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getModelIdentifier() {
        return "gpt-4o";
    }
}
