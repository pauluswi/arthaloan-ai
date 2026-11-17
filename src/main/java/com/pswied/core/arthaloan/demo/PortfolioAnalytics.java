package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioAnalytics(
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate asOfDate,
        String currency,
        AppliedFilters filters,
        Summary summary,
        List<ParMetric> portfolioAtRisk,
        List<DelinquencyBucket> delinquencyDistribution,
        List<MonthlyTrend> repaymentTrend,
        MetricDefinitions definitions,
        FilterOptions filterOptions) {

    public PortfolioAnalytics {
        portfolioAtRisk = List.copyOf(portfolioAtRisk);
        delinquencyDistribution = List.copyOf(delinquencyDistribution);
        repaymentTrend = List.copyOf(repaymentTrend);
    }

    public record AppliedFilters(
            String productId,
            String segment,
            String region,
            String loanStatus) {
    }

    public record Summary(
            long activeLoans,
            BigDecimal outstandingPrincipal,
            BigDecimal scheduledAmount,
            BigDecimal amountCollected,
            BigDecimal repaymentRatePercent) {
    }

    public record ParMetric(
            int thresholdDays,
            long qualifyingLoans,
            BigDecimal atRiskPrincipal,
            BigDecimal portfolioPrincipal,
            BigDecimal ratioPercent) {
    }

    public record DelinquencyBucket(
            String bucket,
            long loanCount,
            BigDecimal outstandingPrincipal) {
    }

    public record MonthlyTrend(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal scheduledAmount,
            BigDecimal amountCollected,
            BigDecimal repaymentRatePercent) {
    }

    public record MetricDefinitions(
            String repaymentRate,
            String dpd,
            String portfolioAtRisk,
            String outstandingPrincipal) {
    }

    public record FilterOptions(
            List<LoanProductOption> products,
            List<String> segments,
            List<String> regions,
            List<String> loanStatuses) {

        public FilterOptions {
            products = List.copyOf(products);
            segments = List.copyOf(segments);
            regions = List.copyOf(regions);
            loanStatuses = List.copyOf(loanStatuses);
        }
    }

    public record LoanProductOption(String productId, String name) {
    }
}
