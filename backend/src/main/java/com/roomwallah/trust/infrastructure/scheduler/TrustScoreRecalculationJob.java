package com.roomwallah.trust.infrastructure.scheduler;

import com.roomwallah.trust.application.service.TrustScoreService;
import com.roomwallah.trust.domain.entity.TrustScore;
import com.roomwallah.trust.domain.port.TrustScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class TrustScoreRecalculationJob {

    private final TrustScoreRepository trustScoreRepository;
    private final TrustScoreService trustScoreService;

    public TrustScoreRecalculationJob(TrustScoreRepository trustScoreRepository, TrustScoreService trustScoreService) {
        this.trustScoreRepository = trustScoreRepository;
        this.trustScoreService = trustScoreService;
    }

    @Scheduled(cron = "${roomwallah.trust.score-recalc.cron:0 0 2 * * ?}")
    public void run() {
        log.info("Starting TrustScoreRecalculationJob...");
        List<TrustScore> scores = trustScoreRepository.findAll();
        for (TrustScore score : scores) {
            try {
                trustScoreService.recalculateTrustScore(score.getUserId(), "DAILY_STALENESS_SWEEP");
            } catch (Exception e) {
                log.error("Failed to recalculate trust score for user: {}", score.getUserId(), e);
            }
        }
        log.info("Finished TrustScoreRecalculationJob.");
    }
}
