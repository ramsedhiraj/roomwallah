package com.roomwallah.search.application.service;

import com.roomwallah.search.domain.port.AutoCompletePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteServiceImpl implements AutoCompleteService {

    private final AutoCompletePort autoCompletePort;

    @Override
    @Cacheable(value = "autocomplete", key = "{#prefix, #city, #limit}", cacheManager = "redisCacheManager", unless = "#result == null || #result.isEmpty()")
    public List<String> suggest(String prefix, String city, int limit) {
        log.debug("Generating autocomplete suggestions - prefix: {}, city: {}, limit: {}", prefix, city, limit);
        return autoCompletePort.suggest(prefix, city, limit);
    }
}
