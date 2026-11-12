package com.power_puff.bms.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU cannot exceed 100 characters")
        String sku,

        @NotBlank(message = "Product title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        @Size(max = 255, message = "Author cannot exceed 255 characters")
        String author,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most eight integer digits and two decimal places")
        BigDecimal price,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @NotNull(message = "Minimum stock is required")
        @Min(value = 0, message = "Minimum stock cannot be negative")
        Integer minStock
) {
}
