package com.roomwallah.common.security;

import com.roomwallah.identity.infrastructure.provider.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZeroTrustService {

    private final DeviceFingerprintService deviceFingerprintService;
    private final ReplayProtectionService replayProtectionService;
    private final SessionRiskEvaluator sessionRiskEvaluator;

    public void validateRequest(HttpServletRequest request) {
        String nonce = request.getHeader("X-Nonce");
        String timestampStr = request.getHeader("X-Timestamp");
        if (nonce != null && timestampStr != null) {
            try {
                long timestamp = Long.parseLong(timestampStr);
                if (!replayProtectionService.validateNonce(nonce, timestamp)) {
                    throw new SecurityException("Replay attack detected or invalid nonce");
                }
            } catch (NumberFormatException e) {
                throw new SecurityException("Invalid X-Timestamp header value");
            }
        }

        UUID userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.user().getId();
        }

        String clientIp = deviceFingerprintService.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String location = request.getHeader("X-Device-Location");

        SessionRiskEvaluator.RiskEvaluationResult riskResult = sessionRiskEvaluator.evaluateRisk(
                userId, clientIp, userAgent, location
        );

        if ("HIGH".equals(riskResult.getStatus())) {
            log.error("High session risk detected for user {}. Triggers: {}", userId, riskResult.getTriggers());
            throw new SecurityException("Continuous authentication check failed due to anomalous session behavior");
        }
    }

    public SessionRiskEvaluator.RiskEvaluationResult getCurrentSessionRisk(HttpServletRequest request) {
        UUID userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.user().getId();
        }

        String clientIp = deviceFingerprintService.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String location = request.getHeader("X-Device-Location");

        return sessionRiskEvaluator.evaluateRisk(userId, clientIp, userAgent, location);
    }
}
