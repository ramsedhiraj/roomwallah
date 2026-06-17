package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.port.SearchEnginePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchEngineRegistry {

    private final List<SearchEnginePort> engines;

    @Value("${roomwallah.search.engine:postgresql}")
    private String preferredEngine;

    public SearchEnginePort resolve() {
        SearchEnginePort preferred = engines.stream()
                .filter(e -> e.providerName().equalsIgnoreCase(preferredEngine))
                .findFirst()
                .orElse(null);

        if (preferred != null && preferred.isAvailable()) {
            return preferred;
        }

        if (preferred != null) {
            log.warn("Preferred search engine '{}' is registered but reports as unavailable. Falling back.", preferredEngine);
        } else {
            log.warn("Preferred search engine '{}' is not registered. Falling back.", preferredEngine);
        }

        return engines.stream()
                .filter(e -> e.providerName().equalsIgnoreCase("postgresql"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PostgreSQL search engine is not registered."));
    }
}
