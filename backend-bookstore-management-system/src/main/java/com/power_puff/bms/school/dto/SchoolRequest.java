package com.power_puff.bms.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRequest(
        @NotBlank(message = "School name is required")
        @Size(max = 255, message = "School name cannot exceed 255 characters")
        String name,

        @Size(max = 255, message = "Contact person cannot exceed 255 characters")
        String contactPerson,

        @Size(max = 50, message = "Phone cannot exceed 50 characters")
        String phone,

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        String address
) {
}
