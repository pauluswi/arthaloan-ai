package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.PortfolioAnalytics.MonthlyTrend;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.Installment;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentAllocation;
import com.pswied.core.arthaloan.demo.SyntheticPortfolio.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioAnalyticsServiceTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 10, 31);

    private final SyntheticPortfolio portfolio =
            new SyntheticPortfolioGenerator(20261117L, AS_OF_DATE).generate();

    private final PortfolioAnalyticsService service = new PortfolioAnalyticsService(
            new DemoPortfolioService(new SyntheticPortfolioGenerator(20261117L, AS_OF_DATE)),
            new LoanInvestigationService(new DemoPortfolioService(
                    new SyntheticPortfolioGenerator(20261117L, AS_OF_DATE))));

    @Test
    void calculatesPeriodAmountsAndRateFromScheduledDueAndPostedAllocations() {
        LocalDate periodStart = LocalDate.of(2026, 10, 1);
        LocalDate periodEnd = LocalDate.of(2026, 10, 31);
        PortfolioAnalytics analytics = service.calculate(periodStart, periodEnd, null, null, null, null);

        BigDecimal expectedScheduled = portfolio.installments().stream()
                .filter(installment -> inPeriod(installment.dueDate(), periodStart, periodEnd))
                .map(Installment::scheduledAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .setScale(2);
        BigDecimal expectedCollected = portfolio.payments().stream()
                .filter(payment -> inPeriod(payment.postingDate(), periodStart, periodEnd))
                .flatMap(payment -> payment.allocations().stream())
                .filter(allocation -> {
                    Installment installment = installment(allocation);
                    return inPeriod(installment.dueDate(), periodStart, periodEnd);
                })
                .map(PaymentAllocation::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .setScale(2);

        assertEquals(12, analytics.summary().activeLoans());
        assertEquals(expectedScheduled, analytics.summary().scheduledAmount());
        assertEquals(expectedCollected, analytics.summary().amountCollected());
        assertEquals(expectedCollected.multiply(BigDecimal.valueOf(100))
                        .divide(expectedScheduled, 2, java.math.RoundingMode.HALF_UP),
                analytics.summary().repaymentRatePercent());
        assertEquals(4, analytics.portfolioAtRisk().size());
        assertEquals(5, analytics.delinquencyDistribution().size());
        assertEquals(6, analytics.repaymentTrend().size());
    }

    @Test
    void filtersPortfolioAndRollsUpParUsingBalanceDenominator() {
        PortfolioAnalytics analytics = service.calculate(
                null, null, "PL-01", "MASS_MARKET", null, "ACTIVE");
        long matchingLoans = portfolio.loans().stream()
                .filter(loan -> loan.productId().equals("PL-01"))
                .filter(loan -> portfolio.customers().stream().anyMatch(customer ->
                        customer.customerId().equals(loan.customerId())
                                && customer.segment().equals("MASS_MARKET")))
                .count();

        assertEquals(matchingLoans, analytics.summary().activeLoans());
        assertEquals("PL-01", analytics.filters().productId());
        assertEquals("MASS_MARKET", analytics.filters().segment());
        assertTrue(analytics.summary().outstandingPrincipal().signum() > 0);
        assertTrue(analytics.portfolioAtRisk().stream()
                .allMatch(par -> par.portfolioPrincipal().equals(analytics.summary().outstandingPrincipal())));
        assertEquals(2, analytics.filterOptions().products().size());
        assertEquals(3, analytics.filterOptions().segments().size());
    }

    @Test
    void usesUnavailableRateWhenPeriodHasNoDueSchedule() {
        PortfolioAnalytics analytics = service.calculate(
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30), null, null, null, null);

        assertEquals(BigDecimal.ZERO.setScale(2), analytics.summary().scheduledAmount());
        assertNull(analytics.summary().repaymentRatePercent());
        MonthlyTrend september = analytics.repaymentTrend().stream()
                .filter(trend -> YearMonth.from(trend.periodStart()).equals(YearMonth.of(2025, 9)))
                .findFirst()
                .orElseThrow();
        assertNull(september.repaymentRatePercent());
    }

    @Test
    void rejectsInvalidDateRangesAndUnknownFilters() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.calculate(AS_OF_DATE, AS_OF_DATE.plusDays(1), null, null, null, null))
                .getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.calculate(LocalDate.of(2026, 11, 1), AS_OF_DATE, null, null, null, null))
                .getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.calculate(null, null, "UNKNOWN", null, null, null))
                .getStatusCode().value());
    }

    private Installment installment(PaymentAllocation allocation) {
        return portfolio.installments().stream()
                .filter(item -> item.installmentId().equals(allocation.installmentId()))
                .findFirst()
                .orElseThrow();
    }

    private static boolean inPeriod(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
