package com.pswied.core.arthaloan.demo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AssistantAnswer(
        String status,
        String question,
        String answer,
        String assistantMode,
        String modelName,
        OffsetDateTime generatedAt,
        Provenance provenance,
        List<ToolCall> toolCalls,
        List<Evidence> evidence) {

    public AssistantAnswer {
        toolCalls = List.copyOf(toolCalls);
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

    public record ToolCall(String toolName, Map<String, String> arguments, String resultSummary) {
        public ToolCall {
            arguments = Map.copyOf(arguments);
        }
    }

    public record Evidence(String label, String value) {
    }
}
