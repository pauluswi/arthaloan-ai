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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoPortfolioControllerTest {

    @Value("${local.server.port}")
    private int port;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void exposesRepeatableSyntheticPortfolioSnapshot() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/api/demo/portfolio".formatted(port)))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode portfolio = objectMapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals("USD", portfolio.path("currency").asText());
        assertEquals(20261117, portfolio.path("seed").asLong());
        assertEquals(12, portfolio.path("customers").size());
        assertEquals(12, portfolio.path("loans").size());
        assertEquals(144, portfolio.path("installments").size());
    }
}
