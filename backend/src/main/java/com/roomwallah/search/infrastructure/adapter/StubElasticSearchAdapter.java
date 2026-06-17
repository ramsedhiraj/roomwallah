package com.roomwallah.search.infrastructure.adapter;

import com.roomwallah.search.domain.model.SearchQuery;
import com.roomwallah.search.domain.port.SearchEnginePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("elasticsearchSearchAdapter")
@Slf4j
public class StubElasticSearchAdapter implements SearchEnginePort {

    @Override
    public SearchResult search(SearchQuery query) {
        log.warn("Elasticsearch search was invoked but Elasticsearch adapter is currently disabled. Stub returning empty results.");
        return new SearchResult(Collections.emptyList(), null, 0L);
    }

    @Override
    public long count(SearchQuery query) {
        log.warn("Elasticsearch count was invoked but Elasticsearch adapter is currently disabled. Stub returning 0.");
        return 0L;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String providerName() {
        return "elasticsearch";
    }
}
