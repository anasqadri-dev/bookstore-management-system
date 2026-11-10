package com.power_puff.bms.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalSales,
        BigDecimal totalAmount,
        List<SalesReportItemResponse> sales
) {
}
