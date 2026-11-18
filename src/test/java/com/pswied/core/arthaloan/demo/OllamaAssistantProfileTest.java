package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.ai.model.chat=ollama",
                "spring.ai.ollama.chat.options.model=test-local-model",
                "spring.ai.ollama.base-url=http://localhost:11434"
        })
class OllamaAssistantProfileTest {

    @Autowired
    private OllamaAssistantService assistantService;

    @Test
    void enablesSpringAiOllamaWithoutConnectingUntilAQuestionIsAsked() {
        OllamaAssistantStatus status = assistantService.status();

        assertTrue(status.enabled());
        assertEquals("test-local-model", status.modelName());
        assertEquals("http://localhost:11434", status.baseUrl());
    }
}
