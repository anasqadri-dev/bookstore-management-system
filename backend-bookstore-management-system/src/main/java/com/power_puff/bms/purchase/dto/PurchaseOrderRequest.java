package com.power_puff.bms.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "Supplier id is required")
        @Positive(message = "Supplier id must be greater than zero")
        Integer supplierId,

        @NotEmpty(message = "A purchase order must contain at least one item")
        List<@NotNull @Valid PurchaseOrderItemRequest> items
) {
}
