package com.pswied.core.arthaloan.demo;

import java.time.OffsetDateTime;
import java.util.List;

public record OllamaAssistantResponse(
        String assistantMode,
        String modelName,
        String answer,
        OffsetDateTime generatedAt,
        String sourceNote,
        List<String> availableTools) {

    public OllamaAssistantResponse {
        availableTools = List.copyOf(availableTools);
    }
}
