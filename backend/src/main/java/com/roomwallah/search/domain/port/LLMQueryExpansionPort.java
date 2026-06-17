package com.roomwallah.search.domain.port;

public interface LLMQueryExpansionPort {

    String expandQuery(String originalQuery);

    boolean isAvailable();
}
