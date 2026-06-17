package com.roomwallah.experiment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperimentCohortRepository extends JpaRepository<ExperimentCohort, UUID> {
    Optional<ExperimentCohort> findByExperimentNameAndUserId(String experimentName, UUID userId);
}
