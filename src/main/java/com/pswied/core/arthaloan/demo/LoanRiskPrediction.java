package com.pswied.core.arthaloan.demo;

import java.time.LocalDate;
import java.util.List;

public record LoanRiskPrediction(
        String loanId,
        LocalDate predictedAt,
        int horizonDays,
        String modelVersion,
        String status,
        Integer score,
        String riskBand,
        String scoreDescription,
        String eligibilityReason,
        List<RiskFactor> factors) {

    public LoanRiskPrediction {
        factors = List.copyOf(factors);
    }

    public record RiskFactor(
            String name,
            String observedValue,
            int points,
            String explanation) {
    }
}
