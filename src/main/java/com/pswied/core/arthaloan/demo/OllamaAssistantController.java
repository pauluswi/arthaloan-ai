package com.pswied.core.arthaloan.demo;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo/ollama")
public class OllamaAssistantController {

    private final ObjectProvider<OllamaAssistantService> assistantServiceProvider;

    public OllamaAssistantController(ObjectProvider<OllamaAssistantService> assistantServiceProvider) {
        this.assistantServiceProvider = assistantServiceProvider;
    }

    @GetMapping("/status")
    public OllamaAssistantStatus status() {
        OllamaAssistantService assistantService = assistantServiceProvider.getIfAvailable();
        return assistantService == null
                ? new OllamaAssistantStatus(false, null, null)
                : assistantService.status();
    }

    @PostMapping("/ask")
    public OllamaAssistantResponse ask(@RequestBody OllamaAssistantRequest request) {
        OllamaAssistantService assistantService = assistantServiceProvider.getIfAvailable();
        if (assistantService == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Local Ollama mode is disabled. Start the app with the ollama profile to enable it.");
        }
        return assistantService.answer(request);
    }
}
