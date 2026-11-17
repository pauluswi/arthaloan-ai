package com.pswied.core.arthaloan.demo;

import com.pswied.core.arthaloan.demo.CollectionWorklist.PriorityFactor;
import com.pswied.core.arthaloan.demo.CollectionWorklist.WorklistItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CollectionWorklistService {

    private static final String POLICY_VERSION = "demo-collection-v1";
    private static final int MAX_DPD_POINTS = 40;
    private static final BigDecimal HIGH_OVERDUE_THRESHOLD = new BigDecimal("2000.00");
    private static final BigDecimal MEDIUM_OVERDUE_THRESHOLD = new BigDecimal("1000.00");
    private static final BigDecimal LOW_OVERDUE_THRESHOLD = new BigDecimal("500.00");

    private final DemoPortfolioService portfolioService;
    private final LoanInvestigationService loanInvestigationService;
    private final LoanRiskPredictionService riskPredictionService;

    public CollectionWorklistService(
            DemoPortfolioService portfolioService,
            LoanInvestigationService loanInvestigationService,
            LoanRiskPredictionService riskPredictionService) {
        this.portfolioService = portfolioService;
        this.loanInvestigationService = loanInvestigationService;
        this.riskPredictionService = riskPredictionService;
    }

    public CollectionWorklist worklist() {
        List<WorklistItem> items = loanInvestigationService.loans().stream()
                .filter(loan -> loan.dpd() > 0 && loan.overdueAmount().signum() > 0)
                .map(loan -> toWorklistItem(loan))
                .sorted(Comparator.comparingInt(WorklistItem::priorityScore).reversed()
                        .thenComparing(Comparator.comparingInt(WorklistItem::dpd).reversed())
                        .thenComparing(WorklistItem::overdueAmount, Comparator.reverseOrder())
                        .thenComparing(WorklistItem::loanId))
                .toList();
        BigDecimal totalOverdue = items.stream()
                .map(WorklistItem::overdueAmount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new CollectionWorklist(
                portfolioService.portfolio().asOfDate(),
                portfolioService.portfolio().currency(),
                POLICY_VERSION,
                items.size(),
                totalOverdue,
                items);
    }

    private WorklistItem toWorklistItem(LoanSummary loan) {
        LoanRiskPrediction risk = riskPredictionService.predict(loan.loanId());
        List<PriorityFactor> factors = new ArrayList<>();

        int dpdPoints = Math.min(MAX_DPD_POINTS, loan.dpd());
        factors.add(new PriorityFactor(
                "Delinquency",
                dpdPoints,
                "%d DPD; 1 point per day, capped at %d.".formatted(loan.dpd(), MAX_DPD_POINTS)));

        int overduePoints = overduePoints(loan.overdueAmount());
        factors.add(new PriorityFactor(
                "Overdue exposure",
                overduePoints,
                "Overdue amount %s.".formatted(loan.overdueAmount().toPlainString())));

        int riskPoints = risk.status().equals("AVAILABLE")
                ? (int) Math.round(risk.score() * 0.4)
                : 0;
        factors.add(new PriorityFactor(
                "30-day risk signal",
                riskPoints,
                risk.status().equals("AVAILABLE")
                        ? "40%% of illustrative risk score %d/100 (%s).".formatted(risk.score(), risk.riskBand())
                        : "Risk signal unavailable; no points assigned."));

        int priorityScore = factors.stream().mapToInt(PriorityFactor::points).sum();
        return new WorklistItem(
                loan.loanId(),
                loan.customerId(),
                loan.productName(),
                loan.segment(),
                loan.region(),
                loan.outstandingPrincipal(),
                loan.overdueAmount(),
                loan.dpd(),
                priorityScore,
                priorityBand(priorityScore),
                risk.score(),
                risk.riskBand(),
                "Review account and recent payment history",
                factors);
    }

    private int overduePoints(BigDecimal overdueAmount) {
        if (overdueAmount.compareTo(HIGH_OVERDUE_THRESHOLD) >= 0) {
            return 20;
        }
        if (overdueAmount.compareTo(MEDIUM_OVERDUE_THRESHOLD) >= 0) {
            return 15;
        }
        if (overdueAmount.compareTo(LOW_OVERDUE_THRESHOLD) >= 0) {
            return 10;
        }
        return 5;
    }

    private String priorityBand(int score) {
        if (score >= 60) {
            return "HIGH";
        }
        if (score >= 35) {
            return "PRIORITY";
        }
        return "STANDARD";
    }
}
