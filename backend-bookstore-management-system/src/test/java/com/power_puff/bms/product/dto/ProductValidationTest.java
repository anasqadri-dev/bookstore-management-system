package com.power_puff.bms.product.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsValidProductIncludingZeroStockThreshold() {
        ProductRequest request = new ProductRequest(
                "BK-100", "Example Book", null, new BigDecimal("125.00"), 0, 0);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankSkuAndTitle() {
        ProductRequest request = new ProductRequest(
                " ", " ", null, new BigDecimal("10.00"), 1, 0);

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sku")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void rejectsZeroOrNegativePrice() {
        ProductRequest zeroPrice = new ProductRequest(
                "BK-101", "Book", null, BigDecimal.ZERO, 1, 0);
        ProductRequest negativePrice = new ProductRequest(
                "BK-102", "Book", null, new BigDecimal("-1.00"), 1, 0);

        assertFalse(validator.validate(zeroPrice).isEmpty());
        assertFalse(validator.validate(negativePrice).isEmpty());
    }

    @Test
    void rejectsNegativeQuantityAndMinimumStock() {
        ProductRequest request = new ProductRequest(
                "BK-103", "Book", null, new BigDecimal("1.00"), -1, -1);

        var violations = validator.validate(request);

        assertEquals(2, violations.size());
    }

    @Test
    void rejectsMissingRequiredValues() {
        ProductRequest request = new ProductRequest(
                null, "Book", null, null, null, null);

        var violations = validator.validate(request);

        assertTrue(violations.size() >= 4);
    }
}
