package com.roomwallah.searcheval;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "eval_timestamp", nullable = false)
    private Instant evalTimestamp;

    @Column(name = "ndcg", nullable = false, precision = 5, scale = 4)
    private BigDecimal ndcg;

    @Column(name = "precision_k", nullable = false, precision = 5, scale = 4)
    private BigDecimal precisionK;

    @Column(name = "recall_k", nullable = false, precision = 5, scale = 4)
    private BigDecimal recallK;

    @Column(name = "ctr", nullable = false, precision = 5, scale = 4)
    private BigDecimal ctr;

    @Column(name = "abandonment_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal abandonmentRate;

    @Column(name = "run_details", columnDefinition = "TEXT")
    private String runDetails;
}
