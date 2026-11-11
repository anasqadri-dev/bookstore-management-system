package com.power_puff.bms.schoolorder.dto;

import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record SchoolOrderResponse(
        Integer id,
        Integer schoolId,
        String schoolName,
        LocalDate orderDate,
        SchoolOrderStatus status,
        String createdBy,
        List<SchoolOrderItemResponse> items
) {
}
