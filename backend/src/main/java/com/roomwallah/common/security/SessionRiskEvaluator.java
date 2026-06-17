package com.roomwallah.common.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionRiskEvaluator {

    private final Map<UUID, UserSessionHistory> sessionHistoryMap = new ConcurrentHashMap<>();

    @Data
    public static class UserSessionHistory {
        private String lastIp;
        private String lastUserAgent;
        private String lastLocation;
        private Instant lastActivityAt;
    }

    @Data
    @Builder
    public static class RiskEvaluationResult {
        private double score; // 0.0 to 1.0
        private String status; // LOW, MEDIUM, HIGH
        private List<String> triggers;
    }

    public RiskEvaluationResult evaluateRisk(UUID userId, String clientIp, String userAgent, String location) {
        if (userId == null) {
            return RiskEvaluationResult.builder()
                    .score(0.0)
                    .status("LOW")
                    .triggers(List.of("Anonymous request"))
                    .build();
        }

        List<String> triggers = new ArrayList<>();
        double score = 0.0;

        UserSessionHistory history = sessionHistoryMap.get(userId);
        if (history == null) {
            history = new UserSessionHistory();
            history.setLastIp(clientIp);
            history.setLastUserAgent(userAgent);
            history.setLastLocation(location);
            history.setLastActivityAt(Instant.now());
            sessionHistoryMap.put(userId, history);

            return RiskEvaluationResult.builder()
                    .score(0.0)
                    .status("LOW")
                    .triggers(List.of("New session initialized"))
                    .build();
        }

        if (!clientIp.equals(history.getLastIp())) {
            score += 0.3;
            triggers.add("IP address changed from " + history.getLastIp() + " to " + clientIp);
        }

        if (userAgent != null && !userAgent.equals(history.getLastUserAgent())) {
            score += 0.4;
            triggers.add("User agent changed");
        }

        if (location != null && history.getLastLocation() != null && !location.equals(history.getLastLocation())) {
            long secondsBetween = Instant.now().getEpochSecond() - history.getLastActivityAt().getEpochSecond();
            if (secondsBetween < 60) {
                score += 0.5;
                triggers.add("Geographic velocity limit exceeded (impossible travel): location changed in " + secondsBetween + "s");
            }
        }

        history.setLastIp(clientIp);
        history.setLastUserAgent(userAgent);
        history.setLastLocation(location);
        history.setLastActivityAt(Instant.now());

        score = Math.min(1.0, score);
        String status = "LOW";
        if (score > 0.7) {
            status = "HIGH";
        } else if (score > 0.3) {
            status = "MEDIUM";
        }

        return RiskEvaluationResult.builder()
                .score(score)
                .status(status)
                .triggers(triggers)
                .build();
    }
}
