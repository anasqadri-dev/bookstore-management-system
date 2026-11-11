package com.power_puff.bms.purchase.dto;

import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        Integer id,
        Integer supplierId,
        String supplierName,
        LocalDate orderDate,
        PurchaseOrderStatus status,
        LocalDate receivedDate,
        String createdBy,
        List<PurchaseOrderItemResponse> items
) {
}
