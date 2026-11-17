package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentDetail(
        String installmentId,
        LocalDate dueDate,
        BigDecimal principalDue,
        BigDecimal interestDue,
        BigDecimal feesDue,
        BigDecimal scheduledAmount,
        BigDecimal allocatedAmount,
        BigDecimal remainingAmount,
        int dpd,
        String status) {
}
