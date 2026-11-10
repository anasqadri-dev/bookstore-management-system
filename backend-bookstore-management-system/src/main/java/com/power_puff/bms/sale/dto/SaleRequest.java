package com.power_puff.bms.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleRequest(
        @Size(max = 255, message = "Customer name cannot exceed 255 characters")
        String customerName,

        @NotEmpty(message = "A sale must contain at least one item")
        List<@NotNull @Valid SaleItemRequest> items
) {
}
