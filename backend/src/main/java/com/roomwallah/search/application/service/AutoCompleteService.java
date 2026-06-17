package com.roomwallah.search.application.service;

import java.util.List;

public interface AutoCompleteService {
    List<String> suggest(String prefix, String city, int limit);
}
