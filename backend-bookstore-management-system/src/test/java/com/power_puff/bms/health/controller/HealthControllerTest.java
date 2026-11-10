package com.power_puff.bms.health.controller;

import com.power_puff.bms.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void healthEndpointReportsBackendAsUp() {
        ApiResponse<java.util.Map<String, String>> response = new HealthController().health();

        assertEquals(true, response.success());
        assertEquals("UP", response.data().get("status"));
    }
}
