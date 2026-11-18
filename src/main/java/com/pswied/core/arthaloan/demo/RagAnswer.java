package com.pswied.core.arthaloan.demo;

import java.time.OffsetDateTime;
import java.util.List;

public record RagAnswer(
        String status,
        String question,
        String answer,
        String retrievalMode,
        String retrievalMethod,
        OffsetDateTime generatedAt,
        List<Source> sources) {

    public RagAnswer {
        sources = List.copyOf(sources);
    }

    public record Source(
            String documentId,
            String title,
            String section,
            String excerpt,
            int matchedTerms) {
    }
}
