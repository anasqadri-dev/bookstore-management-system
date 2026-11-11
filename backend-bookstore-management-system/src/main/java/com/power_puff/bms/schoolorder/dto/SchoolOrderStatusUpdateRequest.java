package com.power_puff.bms.schoolorder.dto;

import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import jakarta.validation.constraints.NotNull;

public record SchoolOrderStatusUpdateRequest(
        @NotNull(message = "School order status is required")
        SchoolOrderStatus status
) {
}
