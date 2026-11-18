package com.pswied.core.arthaloan.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/demo/mcp")
public class MockMcpController {

    private final MockMcpServerService mcpServerService;

    public MockMcpController(MockMcpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @PostMapping
    public ResponseEntity<JsonNode> handle(@RequestBody JsonNode request) {
        if (request != null
                && request.path("jsonrpc").asText().equals("2.0")
                && !request.has("id")
                && request.path("method").asText().startsWith("notifications/")) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(mcpServerService.handle(request));
    }

    @GetMapping("/audit")
    public List<MockMcpServerService.McpAuditEvent> auditEvents() {
        return mcpServerService.auditEvents();
    }
}
