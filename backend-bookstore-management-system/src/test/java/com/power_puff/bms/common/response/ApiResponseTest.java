package com.power_puff.bms.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successResponseContainsPayloadAndMessage() {
        ApiResponse<String> response = ApiResponse.success("Saved", "product");

        assertTrue(response.success());
        assertEquals("Saved", response.message());
        assertEquals("product", response.data());
    }

    @Test
    void errorResponseDoesNotContainPayload() {
        ApiResponse<Void> response = ApiResponse.error("Invalid request");

        assertFalse(response.success());
        assertEquals("Invalid request", response.message());
        assertNull(response.data());
    }
}
