package com.power_puff.bms.schoolorder.mapper;

import com.power_puff.bms.schoolorder.dto.SchoolOrderItemResponse;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;

public final class SchoolOrderMapper {

    private SchoolOrderMapper() {
    }

    public static SchoolOrderResponse toResponse(SchoolOrder order) {
        return new SchoolOrderResponse(
                order.getId(),
                order.getSchool().getId(),
                order.getSchool().getName(),
                order.getOrderDate(),
                order.getStatus(),
                order.getUser().getFullName(),
                order.getItems().stream()
                        .map(SchoolOrderMapper::toItemResponse)
                        .toList());
    }

    private static SchoolOrderItemResponse toItemResponse(SchoolOrderItem item) {
        return new SchoolOrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getUnitPrice());
    }
}
