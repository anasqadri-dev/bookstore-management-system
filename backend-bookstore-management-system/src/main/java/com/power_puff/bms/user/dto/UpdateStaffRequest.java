package com.power_puff.bms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters")
        String newPassword,

        Boolean active
) {
}
