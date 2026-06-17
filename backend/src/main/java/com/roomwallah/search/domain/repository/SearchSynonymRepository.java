package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.SearchSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SearchSynonymRepository extends JpaRepository<SearchSynonym, UUID> {
    Optional<SearchSynonym> findByTermIgnoreCase(String term);
}
