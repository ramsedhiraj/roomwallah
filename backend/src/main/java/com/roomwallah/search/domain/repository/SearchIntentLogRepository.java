package com.roomwallah.search.domain.repository;

import com.roomwallah.search.domain.entity.SearchIntentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SearchIntentLogRepository extends JpaRepository<SearchIntentLog, UUID> {
}
