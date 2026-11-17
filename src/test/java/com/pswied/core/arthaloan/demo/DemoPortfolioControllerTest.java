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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
