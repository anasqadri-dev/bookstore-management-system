package com.power_puff.bms.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseOrderItemRequest(
        @NotNull(message = "Product id is required")
        @Positive(message = "Product id must be greater than zero")
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.01", message = "Unit cost must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Unit cost must have at most two decimal places")
        BigDecimal unitCost
) {
}
