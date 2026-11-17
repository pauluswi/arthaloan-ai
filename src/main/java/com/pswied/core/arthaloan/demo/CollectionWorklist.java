package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CollectionWorklist(
        LocalDate asOfDate,
        String currency,
        String policyVersion,
        int totalAccounts,
        BigDecimal totalOverdueAmount,
        List<WorklistItem> items) {

    public CollectionWorklist {
        items = List.copyOf(items);
    }

    public record WorklistItem(
            String loanId,
            String customerId,
            String productName,
            String segment,
            String region,
            BigDecimal outstandingPrincipal,
            BigDecimal overdueAmount,
            int dpd,
            int priorityScore,
            String priorityBand,
            Integer riskScore,
            String riskBand,
            String suggestedAction,
            List<PriorityFactor> factors) {

        public WorklistItem {
            factors = List.copyOf(factors);
        }
    }

    public record PriorityFactor(String name, int points, String explanation) {
    }
}
