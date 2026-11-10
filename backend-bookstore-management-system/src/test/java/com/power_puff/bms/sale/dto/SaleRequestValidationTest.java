package com.power_puff.bms.sale.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsSaleWithoutItems() {
        SaleRequest request = new SaleRequest(null, List.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("items")));
    }

    @Test
    void rejectsInvalidItemQuantity() {
        SaleRequest request = new SaleRequest(null,
                List.of(new SaleItemRequest(1, 0)));

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().contains("quantity")));
    }

    @Test
    void rejectsNullItemEntries() {
        SaleRequest request = new SaleRequest(null,
                java.util.Arrays.asList((SaleItemRequest) null));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsValidSaleRequest() {
        SaleRequest request = new SaleRequest("Walk-in Customer",
                List.of(new SaleItemRequest(1, 2)));

        assertTrue(validator.validate(request).isEmpty());
    }
}
