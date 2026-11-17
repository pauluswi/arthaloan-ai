package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanRiskPredictionServiceTest {

    private LoanRiskPredictionService service() {
        return new LoanRiskPredictionService(new LoanInvestigationService(
                new DemoPortfolioService(new SyntheticPortfolioGenerator(
                        20261117L, LocalDate.of(2026, 10, 31)))));
    }

    @Test
    void producesDeterministicExplainableRuleScoreForEligibleLoans() {
        LoanRiskPredictionService service = service();
        LoanRiskPrediction performing = service.predict("LN-001");
        LoanRiskPrediction delinquent = service.predict("LN-002");

        assertEquals("AVAILABLE", performing.status());
        assertEquals(0, performing.score());
        assertEquals("LOW", performing.riskBand());
        assertTrue(performing.factors().isEmpty());

        assertEquals(26, delinquent.score());
        assertEquals("MODERATE", delinquent.riskBand());
        assertEquals(LocalDate.of(2026, 10, 31), delinquent.predictedAt());
        assertEquals(30, delinquent.horizonDays());
        assertEquals("demo-rules-v1", delinquent.modelVersion());
        assertTrue(delinquent.factors().stream().anyMatch(factor -> factor.name().equals("Days past due")));
        assertTrue(delinquent.scoreDescription().contains("not a calibrated probability"));

        LoanRiskPrediction reversal = service.predict("LN-007");
        assertEquals(55, reversal.score());
        assertEquals("HIGH", reversal.riskBand());
        assertTrue(reversal.factors().stream().anyMatch(
                factor -> factor.name().equals("Recent payment reversals")));
    }

    @Test
    void reportsUnknownLoanAsNotFound() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service().predict("UNKNOWN")).getStatusCode().value());
    }
}
