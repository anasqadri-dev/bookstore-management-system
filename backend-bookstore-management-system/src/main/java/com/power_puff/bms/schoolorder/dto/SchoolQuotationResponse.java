package com.power_puff.bms.schoolorder.dto;

import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SchoolQuotationResponse(
        Integer orderId,
        Integer schoolId,
        String schoolName,
        LocalDate orderDate,
        SchoolOrderStatus status,
        String createdBy,
        List<SchoolQuotationItemResponse> items,
        BigDecimal totalAmount
) {
}
