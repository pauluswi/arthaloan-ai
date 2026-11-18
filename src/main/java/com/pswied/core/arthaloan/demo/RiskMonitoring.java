package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RiskMonitoring(
        LocalDate asOfDate,
        String currency,
        int horizonDays,
        String modelVersion,
        String scoringMethod,
        int totalLoans,
        int eligibleLoans,
        int lowRiskLoans,
        int moderateRiskLoans,
        int highRiskLoans,
        BigDecimal averageScore,
        List<LoanRiskItem> loans) {

    public RiskMonitoring {
        loans = List.copyOf(loans);
    }

    public record LoanRiskItem(
            String loanId,
            String customerId,
            String productName,
            String segment,
            BigDecimal outstandingPrincipal,
            BigDecimal overdueAmount,
            int dpd,
            String status,
            Integer score,
            String riskBand,
            String eligibilityReason,
            List<LoanRiskPrediction.RiskFactor> factors) {

        public LoanRiskItem {
            factors = List.copyOf(factors);
        }
    }
}
