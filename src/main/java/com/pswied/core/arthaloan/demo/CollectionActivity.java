package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CollectionActivity(
        String activityId,
        String loanId,
        CollectionActivityRequest.ActivityType activityType,
        CollectionActivityRequest.ActivityOutcome outcome,
        Instant recordedAt,
        String recordedBy,
        LocalDate followUpDate,
        BigDecimal promisedAmount,
        String notes) {
}
