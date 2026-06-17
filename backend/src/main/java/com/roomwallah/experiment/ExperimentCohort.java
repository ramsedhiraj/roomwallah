package com.roomwallah.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "experiment_cohorts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentCohort {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cohort", nullable = false, length = 50)
    private String cohort; // CONTROL, TREATMENT

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
