package com.roomwallah.fraud.controller;

import com.roomwallah.identity.infrastructure.provider.UserPrincipal;
import com.roomwallah.fraud.domain.FraudCase;
import com.roomwallah.fraud.domain.FraudRuleSet;
import com.roomwallah.fraud.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/fraud")
@RequiredArgsConstructor
public class AdminFraudController {

    private final FraudService fraudService;

    private UUID getReviewerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.user().getId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    @GetMapping("/cases")
    public ResponseEntity<List<FraudCase>> getCases(@RequestParam(required = false) String status) {
        List<FraudCase> cases = fraudService.getCases(status);
        return ResponseEntity.ok(cases);
    }

    @PostMapping("/evaluate")
    public ResponseEntity<FraudCase> evaluateUser(
            @RequestParam UUID userId,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String deviceToken
    ) {
        FraudCase result = fraudService.evaluateUserRisk(userId, ipAddress, deviceToken);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/cases/{id}/override")
    public ResponseEntity<FraudCase> overrideCase(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam String notes
    ) {
        UUID reviewerId = getReviewerId();
        FraudCase resolved = fraudService.resolveCase(id, reviewerId, status, notes);
        return ResponseEntity.ok(resolved);
    }

    @PutMapping("/cases/{id}/escalate")
    public ResponseEntity<FraudCase> escalateCase(
            @PathVariable UUID id,
            @RequestParam String escalatedTo
    ) {
        FraudCase escalated = fraudService.escalateCase(id, escalatedTo);
        return ResponseEntity.ok(escalated);
    }

    @PostMapping("/rulesets")
    public ResponseEntity<FraudRuleSet> createOrUpdateRuleSet(
            @RequestParam String versionName,
            @RequestParam int velocityLimit,
            @RequestParam BigDecimal largeTransactionLimit,
            @RequestParam(defaultValue = "true") boolean active
    ) {
        FraudRuleSet ruleSet = fraudService.createOrUpdateRuleSet(versionName, velocityLimit, largeTransactionLimit, active);
        return ResponseEntity.ok(ruleSet);
    }
}
