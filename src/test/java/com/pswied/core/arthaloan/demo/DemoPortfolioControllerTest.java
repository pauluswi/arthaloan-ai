package com.pswied.core.arthaloan.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoPortfolioControllerTest {

    @Value("${local.server.port}")
    private int port;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void exposesRepeatableSyntheticPortfolioSnapshot() throws Exception {
        HttpResponse<String> response = get("/api/demo/portfolio");
        JsonNode portfolio = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("USD", portfolio.path("currency").asText());
        assertEquals(20261117, portfolio.path("seed").asLong());
        assertEquals(12, portfolio.path("customers").size());
        assertEquals(12, portfolio.path("loans").size());
        assertEquals(144, portfolio.path("installments").size());
    }

    @Test
    void exposesPaymentAwareLoanInvestigation() throws Exception {
        HttpResponse<String> response = get("/api/demo/loans/LN-002");
        JsonNode loan = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("LN-002", loan.path("loan").path("loanId").asText());
        assertEquals(16, loan.path("dpd").asInt());
        assertEquals(12, loan.path("installments").size());
        assertTrue(loan.path("outstandingPrincipal").decimalValue().signum() > 0);
    }

    @Test
    void returnsNotFoundForUnknownLoan() throws Exception {
        HttpResponse<String> response = get("/api/demo/loans/UNKNOWN");

        assertEquals(404, response.statusCode());
    }

    @Test
    void exposesExplainableLoanRiskScore() throws Exception {
        HttpResponse<String> response = get("/api/demo/loans/LN-002/risk");
        JsonNode prediction = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("LN-002", prediction.path("loanId").asText());
        assertEquals(30, prediction.path("horizonDays").asInt());
        assertEquals("demo-rules-v1", prediction.path("modelVersion").asText());
        assertEquals("MODERATE", prediction.path("riskBand").asText());
        assertTrue(prediction.path("factors").size() > 0);

        assertEquals(404, get("/api/demo/loans/UNKNOWN/risk").statusCode());
    }

    @Test
    void exposesRankedCollectionWorklist() throws Exception {
        HttpResponse<String> response = get("/api/demo/collections/worklist");
        JsonNode worklist = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("demo-collection-v1", worklist.path("policyVersion").asText());
        assertEquals(worklist.path("items").size(), worklist.path("totalAccounts").asInt());
        assertTrue(worklist.path("totalOverdueAmount").decimalValue().signum() > 0);
        assertTrue(worklist.path("items").get(0).path("factors").size() > 0);
    }

    @Test
    void recordsAndReadsSimulatedCollectionActivity() throws Exception {
        String requestBody = """
                {
                  "activityType": "SIMULATED_CONTACT_ATTEMPT",
                  "outcome": "PROMISE_TO_PAY",
                  "followUpDate": "2026-11-15",
                  "promisedAmount": 125.00,
                  "notes": "Synthetic demo record"
                }
                """;
        HttpResponse<String> created = post("/api/demo/loans/LN-002/collection-activities", requestBody);
        JsonNode activity = objectMapper.readTree(created.body());

        assertEquals(201, created.statusCode());
        assertEquals("LN-002", activity.path("loanId").asText());
        assertEquals("demo-user", activity.path("recordedBy").asText());

        HttpResponse<String> historyResponse = get("/api/demo/loans/LN-002/collection-activities");
        JsonNode history = objectMapper.readTree(historyResponse.body());
        assertEquals(200, historyResponse.statusCode());
        assertEquals(1, history.size());
        assertEquals(activity.path("activityId").asText(), history.get(0).path("activityId").asText());

        assertEquals(409, post(
                "/api/demo/loans/LN-001/collection-activities",
                """
                {"activityType":"ACCOUNT_REVIEW","outcome":"ACCOUNT_REVIEWED"}
                """).statusCode());
    }

    @Test
    void exposesPortfolioAnalyticsAndValidatesFilters() throws Exception {
        HttpResponse<String> response = get("/api/demo/analytics?segment=PRIME");
        JsonNode analytics = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("2026-10-01", analytics.path("periodStart").asText());
        assertEquals("2026-10-31", analytics.path("periodEnd").asText());
        assertEquals("PRIME", analytics.path("filters").path("segment").asText());
        assertEquals(4, analytics.path("portfolioAtRisk").size());
        assertEquals(5, analytics.path("delinquencyDistribution").size());

        HttpResponse<String> invalidFilter = get("/api/demo/analytics?segment=UNKNOWN");
        assertEquals(400, invalidFilter.statusCode());
    }

    @Test
    void answersGroundedPortfolioQuestionsWithProvenance() throws Exception {
        HttpResponse<String> response = post(
                "/api/demo/assistant/ask",
                """
                {"question":"What is PAR 30?"}
                """);
        JsonNode answer = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("ANSWERED", answer.path("status").asText());
        assertEquals("SIMULATED", answer.path("assistantMode").asText());
        assertEquals("ArthaLoan mock model v1", answer.path("modelName").asText());
        assertTrue(answer.path("answer").asText().contains("PAR 30"), answer.toPrettyString());
        assertEquals("2026-10-31", answer.path("provenance").path("asOfDate").asText());
        assertTrue(answer.path("provenance").path("metricDefinition").asText().contains("DPD"));
        assertTrue(answer.path("toolCalls").get(0).path("toolName").asText().equals("portfolioAnalytics"));
        assertTrue(answer.path("evidence").size() >= 3);

        HttpResponse<String> segmentResponse = post(
                "/api/demo/assistant/ask",
                """
                {"question":"Which segment contributes most to PAR 30?"}
                """);
        JsonNode segmentAnswer = objectMapper.readTree(segmentResponse.body());
        assertEquals("ANSWERED", segmentAnswer.path("status").asText());
        assertTrue(segmentAnswer.path("evidence").size() >= 2);
    }

    @Test
    void simulatedAssistantUsesCollectionWorklistToolForReviewQuestions() throws Exception {
        JsonNode answer = objectMapper.readTree(post(
                "/api/demo/assistant/ask",
                """
                {"question":"What should I review first in collections?"}
                """).body());

        assertEquals("ANSWERED", answer.path("status").asText());
        assertEquals("collectionWorklist", answer.path("toolCalls").get(0).path("toolName").asText());
        assertTrue(answer.path("toolCalls").get(0).path("resultSummary").asText().contains("demo-collection-v1"));
        assertTrue(answer.path("answer").asText().contains("synthetic account"));
        assertTrue(answer.path("answer").asText().contains("no action is taken"));
        assertTrue(answer.path("evidence").get(0).path("label").asText().contains("First account"));
    }

    @Test
    void localOllamaModeIsDisabledByDefault() throws Exception {
        HttpResponse<String> statusResponse = get("/api/demo/ollama/status");
        JsonNode status = objectMapper.readTree(statusResponse.body());

        assertEquals(200, statusResponse.statusCode());
        assertEquals(false, status.path("enabled").asBoolean());
        assertEquals(503, post(
                "/api/demo/ollama/ask",
                """
                {"question":"Summarize the portfolio"}
                """).statusCode());
    }

    @Test
    void retrievesApprovedKnowledgeWithCitations() throws Exception {
        JsonNode knowledge = objectMapper.readTree(get("/api/demo/rag/knowledge").body());
        assertTrue(knowledge.size() >= 5);

        JsonNode answer = objectMapper.readTree(post(
                "/api/demo/rag/ask",
                """
                {"question":"How is PAR 30 calculated?"}
                """).body());
        assertEquals("ANSWERED", answer.path("status").asText());
        assertEquals("MOCK_RAG", answer.path("retrievalMode").asText());
        assertTrue(answer.path("retrievalMethod").asText().contains("no embeddings"));
        assertTrue(answer.path("answer").asText().contains("inclusive"));
        assertEquals("demo-metric-par", answer.path("sources").get(0).path("documentId").asText());
        assertTrue(answer.path("sources").get(0).path("excerpt").asText().contains("outstanding principal"));
    }

    @Test
    void ragReportsInsufficientKnowledgeAndValidatesQuestion() throws Exception {
        JsonNode unsupported = objectMapper.readTree(post(
                "/api/demo/rag/ask",
                """
                {"question":"What was the 2024 central bank policy rate?"}
                """).body());
        assertEquals("INSUFFICIENT_KNOWLEDGE", unsupported.path("status").asText(), unsupported.toPrettyString());
        assertEquals(0, unsupported.path("sources").size());

        assertEquals(400, post(
                "/api/demo/rag/ask",
                """
                {"question":" "}
                """).statusCode());
    }

    @Test
    void mockMcpNegotiatesProtocolAndListsReadOnlyTools() throws Exception {
        JsonNode initialized = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {
                  "jsonrpc":"2.0",
                  "id":1,
                  "method":"initialize",
                  "params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1"}}
                }
                """).body());
        assertEquals("2025-03-26", initialized.path("result").path("protocolVersion").asText());
        assertTrue(initialized.path("result").path("capabilities").path("tools").isObject());

        JsonNode listed = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                """).body());
        JsonNode tools = listed.path("result").path("tools");
        assertEquals(4, tools.size());
        assertEquals("get_portfolio_summary", tools.get(0).path("name").asText());
        assertEquals("get_loan_snapshot", tools.get(1).path("name").asText());
        assertEquals("get_collection_worklist", tools.get(2).path("name").asText());
        assertEquals("search_approved_knowledge", tools.get(3).path("name").asText());
        assertTrue(tools.get(2).path("inputSchema").path("properties").path("limit").isObject());
    }

    @Test
    void mockMcpCallsAllowlistedToolsAndAuditsWithoutArgumentValues() throws Exception {
        JsonNode portfolio = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":"portfolio","method":"tools/call","params":{"name":"get_portfolio_summary","arguments":{}}}
                """).body());
        JsonNode portfolioResult = portfolio.path("result");
        assertEquals(false, portfolioResult.path("isError").asBoolean());
        assertEquals(12, portfolioResult.path("structuredContent").path("activeLoans").asInt());
        assertTrue(portfolioResult.path("structuredContent").path("par30Percent").isNumber());

        JsonNode loan = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_loan_snapshot","arguments":{"loanId":"LN-002"}}}
                """).body());
        assertEquals("LN-002", loan.path("result").path("structuredContent").path("loanId").asText());

        JsonNode worklist = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_collection_worklist","arguments":{"limit":1}}}
                """).body());
        assertEquals(1, worklist.path("result").path("structuredContent").path("returnedAccounts").asInt());
        assertTrue(worklist.path("result").path("structuredContent").path("hasMore").asBoolean());

        JsonNode knowledge = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"search_approved_knowledge","arguments":{"question":"How is PAR 30 calculated?"}}}
                """).body());
        assertEquals("ANSWERED", knowledge.path("result").path("structuredContent").path("status").asText());
        assertEquals("demo-metric-par", knowledge.path("result").path("structuredContent").path("sources").get(0)
                .path("documentId").asText());

        JsonNode audit = objectMapper.readTree(get("/api/demo/mcp/audit").body());
        JsonNode latest = audit.get(0);
        assertEquals("search_approved_knowledge", latest.path("toolName").asText());
        assertEquals("SUCCESS", latest.path("outcome").asText());
        assertEquals("question", latest.path("inputFields").get(0).asText());
        assertFalse(latest.has("arguments"));
    }

    @Test
    void mockMcpRejectsInvalidToolArgumentsAndUnknownTools() throws Exception {
        JsonNode invalidLoan = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_loan_snapshot","arguments":{"loanId":"other"}}}
                """).body());
        assertTrue(invalidLoan.path("result").path("isError").asBoolean());

        JsonNode invalidLimit = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"get_collection_worklist","arguments":{"limit":11}}}
                """).body());
        assertTrue(invalidLimit.path("result").path("isError").asBoolean());

        JsonNode unknownTool = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"make_payment","arguments":{}}}
                """).body());
        assertTrue(unknownTool.path("result").path("isError").asBoolean());

        JsonNode unknownMethod = objectMapper.readTree(post(
                "/api/demo/mcp",
                """
                {"jsonrpc":"2.0","id":4,"method":"resources/list"}
                """).body());
        assertEquals(-32601, unknownMethod.path("error").path("code").asInt());
    }

    @Test
    void assistantDoesNotInventHistoricalParChangesOrUnsupportedAnswers() throws Exception {
        JsonNode changeAnswer = objectMapper.readTree(post(
                "/api/demo/assistant/ask",
                """
                {"question":"Which segment contributed most to the increase in PAR 30?"}
                """).body());
        assertEquals("INSUFFICIENT_DATA", changeAnswer.path("status").asText());
        assertTrue(changeAnswer.path("answer").asText().contains("only one as-of snapshot"));

        JsonNode trendAnswer = objectMapper.readTree(post(
                "/api/demo/assistant/ask",
                """
                {"question":"Has PAR 30 increased?"}
                """).body());
        assertEquals("INSUFFICIENT_DATA", trendAnswer.path("status").asText());

        JsonNode unsupportedAnswer = objectMapper.readTree(post(
                "/api/demo/assistant/ask",
                """
                {"question":"Should I approve this borrower?"}
                """).body());
        assertEquals("UNSUPPORTED", unsupportedAnswer.path("status").asText());
        assertEquals(0, unsupportedAnswer.path("toolCalls").size());

        assertEquals(400, post(
                "/api/demo/assistant/ask",
                """
                {"question":" "}
                """).statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
