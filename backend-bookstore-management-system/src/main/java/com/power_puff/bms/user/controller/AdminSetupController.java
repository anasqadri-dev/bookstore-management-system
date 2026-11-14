package com.power_puff.bms.user.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.security.AdminProvisioningService;
import com.power_puff.bms.user.dto.AdminSetupRequest;
import com.power_puff.bms.user.dto.SetupStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setup")
public class AdminSetupController {

    private final AdminProvisioningService adminProvisioningService;

    public AdminSetupController(AdminProvisioningService adminProvisioningService) {
        this.adminProvisioningService = adminProvisioningService;
    }

    @GetMapping("/status")
    public ApiResponse<SetupStatusResponse> status() {
        return ApiResponse.success("Setup status retrieved",
                new SetupStatusResponse(adminProvisioningService.isSetupRequired()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createInitialAdmin(
            @Valid @RequestBody AdminSetupRequest request) {
        adminProvisioningService.provisionInitialAdmin(
                request.username(), request.password(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Admin account created. Sign in with your new credentials."));
    }
}
