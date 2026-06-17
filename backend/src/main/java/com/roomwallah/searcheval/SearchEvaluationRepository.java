package com.roomwallah.searcheval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchEvaluationRepository extends JpaRepository<SearchEvaluation, UUID> {
}
