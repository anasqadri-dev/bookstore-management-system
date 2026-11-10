package com.power_puff.bms.schoolorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record SchoolOrderRequest(
        @NotNull(message = "School id is required")
        @Positive(message = "School id must be greater than zero")
        Integer schoolId,

        @NotEmpty(message = "A school order must contain at least one item")
        List<@NotNull @Valid SchoolOrderItemRequest> items
) {
}
