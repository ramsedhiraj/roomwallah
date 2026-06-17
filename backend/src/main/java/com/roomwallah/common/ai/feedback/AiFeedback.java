package com.roomwallah.common.ai.feedback;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // CHAT_MESSAGE, RECOMMENDATION, SEARCH

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_positive", nullable = false)
    private boolean isPositive;

    @Column(name = "issue_report", columnDefinition = "TEXT")
    private String issueReport;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;
}
