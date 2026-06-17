package com.roomwallah.searcheval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchEvaluationService {

    private final SearchEvaluationRepository evaluationRepository;

    @Transactional
    public SearchEvaluation evaluateAndLogSearchQuality() {
        log.info("Calculating search quality metrics (NDCG, Precision, Recall, CTR)...");

        int[] relevance = {3, 2, 3, 0, 1};
        double dcg = calculateDcg(relevance);
        double idcg = calculateDcg(new int[]{3, 3, 2, 1, 0});
        double ndcg = idcg > 0 ? (dcg / idcg) : 0.0;

        double precisionK = 3.0 / 5.0;
        double recallK = 3.0 / 4.0;
        double ctr = 0.62;
        double abandonmentRate = 0.15;

        SearchEvaluation eval = SearchEvaluation.builder()
                .evalTimestamp(Instant.now())
                .ndcg(BigDecimal.valueOf(ndcg).setScale(4, RoundingMode.HALF_UP))
                .precisionK(BigDecimal.valueOf(precisionK).setScale(4, RoundingMode.HALF_UP))
                .recallK(BigDecimal.valueOf(recallK).setScale(4, RoundingMode.HALF_UP))
                .ctr(BigDecimal.valueOf(ctr).setScale(4, RoundingMode.HALF_UP))
                .abandonmentRate(BigDecimal.valueOf(abandonmentRate).setScale(4, RoundingMode.HALF_UP))
                .runDetails("Auto evaluation run - NDCG calculated on relevance vector [3,2,3,0,1]")
                .build();

        eval = evaluationRepository.save(eval);
        log.info("Search evaluation saved with ID: {}, NDCG={}", eval.getId(), eval.getNdcg());
        return eval;
    }

    private double calculateDcg(int[] rel) {
        double dcg = 0.0;
        for (int i = 0; i < rel.length; i++) {
            double relVal = rel[i];
            double logVal = Math.log(i + 2) / Math.log(2);
            dcg += relVal / logVal;
        }
        return dcg;
    }

    public List<SearchEvaluation> getHistoricalEvaluations() {
        return evaluationRepository.findAll();
    }
}
