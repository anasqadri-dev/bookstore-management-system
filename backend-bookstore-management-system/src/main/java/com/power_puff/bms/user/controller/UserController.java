package com.power_puff.bms.user.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.user.dto.CreateStaffRequest;
import com.power_puff.bms.user.dto.UpdateStaffRequest;
import com.power_puff.bms.user.dto.UserResponse;
import com.power_puff.bms.user.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.success("Users retrieved successfully", userAccountService.listUsers());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        UserResponse response = userAccountService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff account created successfully", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateStaff(
            @PathVariable Long id, @Valid @RequestBody UpdateStaffRequest request) {
        return ApiResponse.success("Staff account updated successfully",
                userAccountService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UserResponse> deactivateStaff(@PathVariable Long id) {
        return ApiResponse.success("Staff account deactivated successfully",
                userAccountService.deactivateStaff(id));
    }
}
