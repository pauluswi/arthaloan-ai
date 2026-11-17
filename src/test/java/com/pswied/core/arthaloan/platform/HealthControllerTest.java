package com.pswied.core.arthaloan.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void reportsTheApiAsUp() {
        HealthController.HealthResponse response = new HealthController().health();

        assertEquals("UP", response.status());
        assertEquals("arthaloan-ai-api", response.service());
    }
}
