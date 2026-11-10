package com.power_puff.bms.user.dto;

import com.power_puff.bms.user.entity.UserRole;

public record LoginResponse(
        Long id,
        String username,
        String fullName,
        UserRole role
) {
}
