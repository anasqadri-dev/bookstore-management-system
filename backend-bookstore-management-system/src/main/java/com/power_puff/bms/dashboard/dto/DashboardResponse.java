package com.power_puff.bms.dashboard.dto;

import com.power_puff.bms.report.dto.SalesReportItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalBooks,
        BigDecimal todaySales,
        long lowStock,
        long suppliers,
        List<SalesReportItemResponse> recentSales
) {
}
