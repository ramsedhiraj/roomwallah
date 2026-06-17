package com.roomwallah;

import com.roomwallah.searcheval.SearchEvaluation;
import com.roomwallah.searcheval.SearchEvaluationRepository;
import com.roomwallah.searcheval.SearchEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SearchEvaluationTest {

    @Mock
    private SearchEvaluationRepository evaluationRepository;

    private SearchEvaluationService searchEvaluationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        searchEvaluationService = new SearchEvaluationService(evaluationRepository);
    }

    @Test
    public void testNdcgCalculationsAndLogging() {
        when(evaluationRepository.save(any(SearchEvaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SearchEvaluation eval = searchEvaluationService.evaluateAndLogSearchQuality();

        assertNotNull(eval);
        assertNotNull(eval.getEvalTimestamp());
        
        // Assert NDCG value is between 0 and 1
        double ndcgVal = eval.getNdcg().doubleValue();
        assertTrue(ndcgVal >= 0.0 && ndcgVal <= 1.0, "NDCG should be in range [0, 1]");
        
        // Assert other precision metrics
        assertEquals(0.60, eval.getPrecisionK().doubleValue(), 0.01);
        assertEquals(0.75, eval.getRecallK().doubleValue(), 0.01);
        assertEquals(0.62, eval.getCtr().doubleValue(), 0.01);
        assertEquals(0.15, eval.getAbandonmentRate().doubleValue(), 0.01);

        verify(evaluationRepository, times(1)).save(any(SearchEvaluation.class));
    }
}
