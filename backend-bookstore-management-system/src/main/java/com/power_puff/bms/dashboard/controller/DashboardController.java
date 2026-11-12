package com.power_puff.bms.dashboard.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.dashboard.dto.DashboardResponse;
import com.power_puff.bms.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard retrieved successfully.",
                dashboardService.getDashboard()));
    }
}
