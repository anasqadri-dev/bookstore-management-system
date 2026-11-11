package com.power_puff.bms.purchase.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderRequestValidationTest {

    private final Validator validator;

    PurchaseOrderRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void rejectsMissingSupplierAndEmptyItems() {
        assertFalse(validator.validate(new PurchaseOrderRequest(null, List.of())).isEmpty());
        assertFalse(validator.validate(new PurchaseOrderRequest(1, List.of())).isEmpty());
    }

    @Test
    void rejectsNonPositiveProductIdsAndQuantities() {
        PurchaseOrderRequest invalidProduct = new PurchaseOrderRequest(
                1, List.of(new PurchaseOrderItemRequest(0, 1, new BigDecimal("5.00"))));
        PurchaseOrderRequest invalidQuantity = new PurchaseOrderRequest(
                1, List.of(new PurchaseOrderItemRequest(1, 0, new BigDecimal("5.00"))));

        assertFalse(validator.validate(invalidProduct).isEmpty());
        assertFalse(validator.validate(invalidQuantity).isEmpty());
    }

    @Test
    void rejectsMissingNonPositiveOrOverPrecisionUnitCost() {
        for (BigDecimal invalidCost : Arrays.asList(
                null, BigDecimal.ZERO, new BigDecimal("-1.00"), new BigDecimal("1.234"))) {
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    1, List.of(new PurchaseOrderItemRequest(1, 1, invalidCost)));

            assertFalse(validator.validate(request).isEmpty(),
                    "Invalid unit cost should be rejected: " + invalidCost);
        }
    }

    @Test
    void rejectsNullItemsAndAcceptsValidOrder() {
        PurchaseOrderRequest nullItem = new PurchaseOrderRequest(
                1, Arrays.asList((PurchaseOrderItemRequest) null));
        PurchaseOrderRequest valid = new PurchaseOrderRequest(
                4,
                List.of(new PurchaseOrderItemRequest(
                        7, 5, new BigDecimal("32.50"))));

        assertFalse(validator.validate(nullItem).isEmpty());
        assertTrue(validator.validate(valid).isEmpty());
    }
}
