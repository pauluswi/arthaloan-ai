package com.pswied.core.arthaloan.demo;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
public class MockMcpServerService {

    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final int MAX_AUDIT_EVENTS = 50;
    private static final int DEFAULT_WORKLIST_LIMIT = 5;
    private static final int MAX_WORKLIST_LIMIT = 10;
    private static final Pattern LOAN_ID_PATTERN = Pattern.compile("LN-\\d{3}");

    private final ObjectMapper objectMapper;
    private final PortfolioAnalyticsService analyticsService;
    private final LoanInvestigationService loanInvestigationService;
    private final CollectionWorklistService collectionWorklistService;
    private final MockRagService ragService;
    private final Deque<McpAuditEvent> auditEvents = new ConcurrentLinkedDeque<>();
    private final AtomicLong auditSequence = new AtomicLong();

    public MockMcpServerService(
            ObjectMapper objectMapper,
            PortfolioAnalyticsService analyticsService,
            LoanInvestigationService loanInvestigationService,
            CollectionWorklistService collectionWorklistService,
            MockRagService ragService) {
        this.objectMapper = objectMapper;
        this.analyticsService = analyticsService;
        this.loanInvestigationService = loanInvestigationService;
        this.collectionWorklistService = collectionWorklistService;
        this.ragService = ragService;
    }

    public JsonNode handle(JsonNode request) {
        if (request == null
                || !request.isObject()
                || !"2.0".equals(request.path("jsonrpc").asText())
                || !request.path("method").isTextual()) {
            return error(null, -32600, "Invalid Request");
        }

        JsonNode id = request.get("id");
        if (id == null || id.isNull()) {
            return error(null, -32600, "Invalid Request");
        }

        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        return switch (method) {
            case "initialize" -> success(id, initialize(params));
            case "tools/list" -> success(id, Map.of("tools", tools()));
            case "tools/call" -> callTool(id, params);
            default -> error(id, -32601, "Method not found");
        };
    }

    public List<McpAuditEvent> auditEvents() {
        return List.copyOf(auditEvents);
    }

    private Map<String, Object> initialize(JsonNode params) {
        String requestedVersion = params.path("protocolVersion").asText();
        String negotiatedVersion = requestedVersion.equals(PROTOCOL_VERSION)
                ? requestedVersion
                : PROTOCOL_VERSION;
        return Map.of(
                "protocolVersion", negotiatedVersion,
                "capabilities", Map.of("tools", Map.of("listChanged", false)),
                "serverInfo", Map.of("name", "ArthaLoan Mock MCP Server", "version", "1.0.0"));
    }

    private List<Map<String, Object>> tools() {
        return List.of(
                tool(
                        "get_portfolio_summary",
                        "Read synthetic portfolio totals and PAR 30. No filters or account-level data are returned.",
                        Map.of(),
                        List.of()),
                tool(
                        "get_loan_snapshot",
                        "Read a summary of one synthetic loan by its demo loan ID.",
                        Map.of("loanId", Map.of(
                                "type", "string",
                                "pattern", "^LN-[0-9]{3}$",
                                "description", "Synthetic demo loan ID, for example LN-002.")),
                        List.of("loanId")),
                tool(
                        "get_collection_worklist",
                        "Read a limited number of synthetic overdue accounts ranked for human review; this tool takes no action.",
                        Map.of("limit", Map.of(
                                "type", "integer",
                                "minimum", 1,
                                "maximum", MAX_WORKLIST_LIMIT,
                                "default", DEFAULT_WORKLIST_LIMIT)),
                        List.of()),
                tool(
                        "search_approved_knowledge",
                        "Search the curated synthetic demo knowledge base and return cited source excerpts.",
                        Map.of("question", Map.of(
                                "type", "string",
                                "minLength", 1,
                                "maxLength", 500)),
                        List.of("question")));
    }

    private Map<String, Object> tool(
            String name,
            String description,
            Map<String, Object> properties,
            List<String> required) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", required,
                        "additionalProperties", false));
    }

    private JsonNode callTool(JsonNode id, JsonNode params) {
        String name = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        if (name.isBlank()) {
            return error(id, -32602, "Tool name is required");
        }

        List<String> inputFields = argumentFields(arguments);
        try {
            Object result = switch (name) {
                case "get_portfolio_summary" -> portfolioSummary(arguments);
                case "get_loan_snapshot" -> loanSnapshot(arguments);
                case "get_collection_worklist" -> collectionWorklist(arguments);
                case "search_approved_knowledge" -> searchKnowledge(arguments);
                default -> throw new IllegalArgumentException("Tool is not allowlisted.");
            };
            recordAudit(name, "SUCCESS", inputFields);
            return success(id, toolResult(result, null));
        } catch (IllegalArgumentException exception) {
            recordAudit(name, "REJECTED", inputFields);
            return success(id, toolResult(null, exception.getMessage()));
        }
    }

    private McpPortfolioSummary portfolioSummary(JsonNode arguments) {
        validateFields(arguments, Set.of());
        PortfolioAnalytics analytics = analyticsService.calculate(null, null, null, null, null, null);
        PortfolioAnalytics.ParMetric par30 = analytics.portfolioAtRisk().stream()
                .filter(metric -> metric.thresholdDays() == 30)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PAR 30 metric is unavailable."));
        return new McpPortfolioSummary(
                analytics.asOfDate(),
                analytics.currency(),
                analytics.summary().activeLoans(),
                analytics.summary().outstandingPrincipal(),
                analytics.summary().repaymentRatePercent(),
                par30.ratioPercent(),
                par30.atRiskPrincipal());
    }

    private LoanSummary loanSnapshot(JsonNode arguments) {
        validateFields(arguments, Set.of("loanId"));
        String loanId = requiredText(arguments, "loanId", 20);
        if (!LOAN_ID_PATTERN.matcher(loanId).matches()) {
            throw new IllegalArgumentException("loanId must be a synthetic ID such as LN-002.");
        }
        return loanInvestigationService.loans().stream()
                .filter(loan -> loan.loanId().equals(loanId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No synthetic loan was found for that loanId."));
    }

    private McpWorklistSummary collectionWorklist(JsonNode arguments) {
        validateFields(arguments, Set.of("limit"));
        JsonNode limitNode = arguments.path("limit");
        int limit = arguments.has("limit") ? limitNode.asInt(-1) : DEFAULT_WORKLIST_LIMIT;
        if (limit < 1 || limit > MAX_WORKLIST_LIMIT
                || (arguments.has("limit") && !limitNode.isIntegralNumber())) {
            throw new IllegalArgumentException("limit must be an integer from 1 to " + MAX_WORKLIST_LIMIT + ".");
        }

        CollectionWorklist worklist = collectionWorklistService.worklist();
        List<CollectionWorklist.WorklistItem> items = worklist.items().stream().limit(limit).toList();
        return new McpWorklistSummary(
                worklist.asOfDate(),
                worklist.currency(),
                worklist.policyVersion(),
                worklist.totalAccounts(),
                worklist.totalOverdueAmount(),
                items.size(),
                items,
                items.size() < worklist.totalAccounts());
    }

    private RagAnswer searchKnowledge(JsonNode arguments) {
        validateFields(arguments, Set.of("question"));
        String question = requiredText(arguments, "question", 500);
        return ragService.answer(new RagQuestion(question));
    }

    private String requiredText(JsonNode arguments, String name, int maxLength) {
        JsonNode value = arguments.path(name);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        String normalized = value.asText().strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters.");
        }
        return normalized;
    }

    private void validateFields(JsonNode arguments, Set<String> allowedFields) {
        if (!arguments.isMissingNode() && !arguments.isObject()) {
            throw new IllegalArgumentException("Tool arguments must be an object.");
        }
        List<String> unexpected = new ArrayList<>();
        arguments.properties().forEach(property -> {
            String field = property.getKey();
            if (!allowedFields.contains(field)) {
                unexpected.add(field);
            }
        });
        if (!unexpected.isEmpty()) {
            throw new IllegalArgumentException("Unexpected tool argument.");
        }
    }

    private ObjectNode toolResult(Object result, String errorMessage) {
        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("content").addObject()
                .put("type", "text")
                .put("text", errorMessage == null ? objectMapper.valueToTree(result).toString() : errorMessage);
        response.put("isError", errorMessage != null);
        if (errorMessage == null) {
            response.set("structuredContent", objectMapper.valueToTree(result));
        }
        return response;
    }

    private List<String> argumentFields(JsonNode arguments) {
        if (!arguments.isObject()) {
            return List.of();
        }
        List<String> fields = new ArrayList<>();
        arguments.properties().forEach(property -> fields.add(property.getKey()));
        fields.sort(String::compareTo);
        return List.copyOf(fields);
    }

    private void recordAudit(String toolName, String outcome, List<String> inputFields) {
        auditEvents.addFirst(new McpAuditEvent(
                auditSequence.incrementAndGet(),
                toolName,
                outcome,
                inputFields,
                OffsetDateTime.now()));
        while (auditEvents.size() > MAX_AUDIT_EVENTS) {
            auditEvents.pollLast();
        }
    }

    private JsonNode success(JsonNode id, Object result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", objectMapper.valueToTree(result));
        return response;
    }

    private JsonNode error(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id == null) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        response.putObject("error").put("code", code).put("message", message);
        return response;
    }

    public record McpAuditEvent(
            long sequence,
            String toolName,
            String outcome,
            List<String> inputFields,
            OffsetDateTime recordedAt) {

        public McpAuditEvent {
            inputFields = List.copyOf(inputFields);
        }
    }

    private record McpPortfolioSummary(
            java.time.LocalDate asOfDate,
            String currency,
            long activeLoans,
            java.math.BigDecimal outstandingPrincipal,
            java.math.BigDecimal repaymentRatePercent,
            java.math.BigDecimal par30Percent,
            java.math.BigDecimal par30Principal) {
    }

    private record McpWorklistSummary(
            java.time.LocalDate asOfDate,
            String currency,
            String policyVersion,
            int totalAccounts,
            java.math.BigDecimal totalOverdueAmount,
            int returnedAccounts,
            List<CollectionWorklist.WorklistItem> items,
            boolean hasMore) {

        private McpWorklistSummary {
            items = List.copyOf(items);
        }
    }
}
