package com.pswied.core.arthaloan.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo/rag")
public class RagDemoController {

    private final MockRagService ragService;

    public RagDemoController(MockRagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/knowledge")
    public List<MockRagService.KnowledgeSummary> knowledgeSources() {
        return ragService.knowledgeSources();
    }

    @PostMapping("/ask")
    public RagAnswer ask(@RequestBody RagQuestion request) {
        return ragService.answer(request);
    }
}
