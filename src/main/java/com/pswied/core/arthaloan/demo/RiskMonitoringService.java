package com.pswied.core.arthaloan.demo;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class RiskMonitoringService {

    private final LoanInvestigationService loanInvestigationService;
    private final LoanRiskPredictionService riskPredictionService;
    private final DemoPortfolioService portfolioService;

    public RiskMonitoringService(
            LoanInvestigationService loanInvestigationService,
            LoanRiskPredictionService riskPredictionService,
            DemoPortfolioService portfolioService) {
        this.loanInvestigationService = loanInvestigationService;
        this.riskPredictionService = riskPredictionService;
        this.portfolioService = portfolioService;
    }

    public RiskMonitoring monitoring() {
        List<RiskMonitoring.LoanRiskItem> items = loanInvestigationService.loans().stream()
                .map(loan -> {
                    LoanRiskPrediction prediction = riskPredictionService.predict(loan.loanId());
                    return new RiskMonitoring.LoanRiskItem(
                            loan.loanId(),
                            loan.customerId(),
                            loan.productName(),
                            loan.segment(),
                            loan.outstandingPrincipal(),
                            loan.overdueAmount(),
                            loan.dpd(),
                            prediction.status(),
                            prediction.score(),
                            prediction.riskBand(),
                            prediction.eligibilityReason(),
                            prediction.factors());
                })
                .sorted(Comparator.comparing(
                                RiskMonitoring.LoanRiskItem::score,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RiskMonitoring.LoanRiskItem::loanId))
                .toList();

        List<RiskMonitoring.LoanRiskItem> eligible = items.stream()
                .filter(item -> item.score() != null)
                .toList();
        BigDecimal averageScore = eligible.isEmpty()
                ? null
                : eligible.stream()
                        .map(item -> BigDecimal.valueOf(item.score()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(eligible.size()), 2, RoundingMode.HALF_UP);

        SyntheticPortfolio portfolio = portfolioService.portfolio();
        return new RiskMonitoring(
                portfolio.asOfDate(),
                portfolio.currency(),
                30,
                "demo-rules-v1",
                "Rule-based demo signal; not a trained or validated predictive model.",
                items.size(),
                eligible.size(),
                countBand(eligible, "LOW"),
                countBand(eligible, "MODERATE"),
                countBand(eligible, "HIGH"),
                averageScore,
                items);
    }

    private int countBand(List<RiskMonitoring.LoanRiskItem> loans, String band) {
        return Math.toIntExact(loans.stream()
                .filter(item -> item.riskBand().equals(band))
                .count());
    }
}
