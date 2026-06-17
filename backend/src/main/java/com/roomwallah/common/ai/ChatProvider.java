package com.roomwallah.common.ai;

import java.util.List;
import java.util.Map;

public interface ChatProvider {
    String chat(List<Map<String, String>> history, String userMessage);
    boolean isAvailable();
    String getModelIdentifier();
}
