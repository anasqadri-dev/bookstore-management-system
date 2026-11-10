package com.power_puff.bms.schoolorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SchoolOrderItemRequest(
        @NotNull(message = "Product id is required")
        @Positive(message = "Product id must be greater than zero")
        Integer productId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {
}
