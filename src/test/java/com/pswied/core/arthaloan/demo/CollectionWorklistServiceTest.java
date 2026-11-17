package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionWorklistServiceTest {

    private CollectionWorklistService service() {
        DemoPortfolioService portfolio = new DemoPortfolioService(
                new SyntheticPortfolioGenerator(20261117L, LocalDate.of(2026, 10, 31)));
        LoanInvestigationService investigations = new LoanInvestigationService(portfolio);
        LoanRiskPredictionService risk = new LoanRiskPredictionService(investigations);
        return new CollectionWorklistService(portfolio, investigations, risk);
    }

    @Test
    void producesDeterministicRankedWorklistWithExplainableFactors() {
        CollectionWorklist worklist = service().worklist();

        assertEquals(LocalDate.of(2026, 10, 31), worklist.asOfDate());
        assertEquals("USD", worklist.currency());
        assertEquals("demo-collection-v1", worklist.policyVersion());
        assertEquals(worklist.items().size(), worklist.totalAccounts());
        assertTrue(worklist.totalAccounts() > 0);
        assertTrue(worklist.items().stream().allMatch(item -> item.dpd() > 0 && item.overdueAmount().signum() > 0));
        assertTrue(worklist.items().stream().allMatch(item ->
                item.factors().stream().mapToInt(CollectionWorklist.PriorityFactor::points).sum()
                        == item.priorityScore()));
        assertTrue(worklist.items().stream().allMatch(item -> item.factors().size() == 3));

        for (int index = 1; index < worklist.items().size(); index++) {
            assertTrue(worklist.items().get(index - 1).priorityScore()
                    >= worklist.items().get(index).priorityScore());
        }

        BigDecimal expectedOverdue = worklist.items().stream()
                .map(CollectionWorklist.WorklistItem::overdueAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .setScale(2);
        assertEquals(expectedOverdue, worklist.totalOverdueAmount());
    }
}
