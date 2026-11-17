package com.pswied.core.arthaloan.demo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AssistantAnswer(
        String status,
        String question,
        String answer,
        OffsetDateTime generatedAt,
        Provenance provenance,
        List<Evidence> evidence) {

    public AssistantAnswer {
        evidence = List.copyOf(evidence);
    }

    public record Provenance(
            String source,
            String metricDefinition,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate asOfDate,
            PortfolioAnalytics.AppliedFilters filters) {
    }

    public record Evidence(String label, String value) {
    }
}
