package com.power_puff.bms.common.exception;

import com.power_puff.bms.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourceToNotFound() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Product was not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("Product was not found", response.getBody().message());
    }

    @Test
    void mapsDuplicateResourceToConflict() {
        var response = handler.handleDuplicate(new DuplicateResourceException("Duplicate SKU"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().success());
    }
}
