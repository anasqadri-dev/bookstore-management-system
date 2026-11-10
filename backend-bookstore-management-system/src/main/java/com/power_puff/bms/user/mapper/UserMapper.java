package com.power_puff.bms.user.mapper;

import com.power_puff.bms.user.dto.LoginResponse;
import com.power_puff.bms.user.dto.UserResponse;
import com.power_puff.bms.user.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static LoginResponse toLoginResponse(User user) {
        return new LoginResponse(user.getId(), user.getUsername(),
                user.getFullName(), user.getRole());
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(),
                user.getFullName(), user.getRole(), user.isActive(),
                user.getCreatedAt());
    }
}
