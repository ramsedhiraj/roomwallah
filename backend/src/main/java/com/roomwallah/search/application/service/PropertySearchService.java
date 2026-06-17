package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.SearchEnginePort.SearchResult;

import java.util.UUID;

public interface PropertySearchService {
    SearchResult search(SearchQuery query, UUID userId, String correlationId);
    SearchResult search(SearchQuery query, UUID userId);
    long count(SearchQuery query);
}
