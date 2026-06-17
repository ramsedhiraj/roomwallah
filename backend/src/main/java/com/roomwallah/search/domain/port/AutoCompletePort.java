package com.roomwallah.search.domain.port;

import java.util.List;

public interface AutoCompletePort {

    List<String> suggest(String prefix, String city, int limit);
}
