package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanSummary(
        String loanId,
        String customerId,
        String segment,
        String region,
        String productId,
        String productName,
        BigDecimal principal,
        BigDecimal outstandingPrincipal,
        String currency,
        String status,
        int dpd,
        BigDecimal overdueAmount,
        LocalDate asOfDate) {
}
