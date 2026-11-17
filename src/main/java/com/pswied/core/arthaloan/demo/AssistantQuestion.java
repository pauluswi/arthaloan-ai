package com.pswied.core.arthaloan.demo;

import java.time.LocalDate;

public record AssistantQuestion(
        String question,
        LocalDate periodStart,
        LocalDate periodEnd,
        String productId,
        String segment,
        String region,
        String loanStatus) {
}
