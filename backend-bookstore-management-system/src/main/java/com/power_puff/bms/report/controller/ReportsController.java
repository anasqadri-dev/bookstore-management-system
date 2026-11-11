package com.power_puff.bms.report.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.report.dto.InventoryReportResponse;
import com.power_puff.bms.report.dto.SalesReportResponse;
import com.power_puff.bms.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sales report generated successfully.",
                reportService.getSalesReport(startDate, endDate)));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventoryReport() {
        return ResponseEntity.ok(ApiResponse.success(
                "Inventory report generated successfully.",
                reportService.getInventoryReport()));
    }
}
