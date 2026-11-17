package com.pswied.core.arthaloan.demo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectionActivityRequest(
        ActivityType activityType,
        ActivityOutcome outcome,
        LocalDate followUpDate,
        BigDecimal promisedAmount,
        String notes) {

    public enum ActivityType {
        ACCOUNT_REVIEW,
        SIMULATED_CONTACT_ATTEMPT,
        FOLLOW_UP
    }

    public enum ActivityOutcome {
        ACCOUNT_REVIEWED,
        NO_RESPONSE_SIMULATED,
        PROMISE_TO_PAY,
        FOLLOW_UP_REQUIRED,
        PAYMENT_REPORTED,
        DISPUTE_REPORTED
    }
}
