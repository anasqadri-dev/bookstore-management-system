package com.power_puff.bms.schoolorder.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolOrderRequestValidationTest {

    private final Validator validator;

    SchoolOrderRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void rejectsMissingSchoolAndEmptyItems() {
        assertFalse(validator.validate(new SchoolOrderRequest(null, List.of())).isEmpty());
        assertFalse(validator.validate(new SchoolOrderRequest(1, List.of())).isEmpty());
    }

    @Test
    void rejectsNonPositiveProductIdsAndQuantities() {
        SchoolOrderRequest invalidProduct = new SchoolOrderRequest(
                1, List.of(new SchoolOrderItemRequest(0, 1)));
        SchoolOrderRequest invalidQuantity = new SchoolOrderRequest(
                1, List.of(new SchoolOrderItemRequest(1, 0)));

        assertFalse(validator.validate(invalidProduct).isEmpty());
        assertFalse(validator.validate(invalidQuantity).isEmpty());
    }

    @Test
    void rejectsNullItemEntries() {
        SchoolOrderRequest request = new SchoolOrderRequest(
                1, Arrays.asList((SchoolOrderItemRequest) null));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsAValidSchoolOrderRequest() {
        SchoolOrderRequest request = new SchoolOrderRequest(
                10,
                List.of(new SchoolOrderItemRequest(25, 4)));

        assertTrue(validator.validate(request).isEmpty());
    }
}
