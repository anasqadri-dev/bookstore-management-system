package com.power_puff.bms.user.dto;

import com.power_puff.bms.user.entity.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        UserRole role,
        boolean active,
        Instant createdAt
) {
}
