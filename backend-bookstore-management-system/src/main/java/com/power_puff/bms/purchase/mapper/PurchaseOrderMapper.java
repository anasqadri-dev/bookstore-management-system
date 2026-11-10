package com.power_puff.bms.purchase.mapper;

import com.power_puff.bms.purchase.dto.PurchaseOrderItemResponse;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderItem;

public final class PurchaseOrderMapper {

    private PurchaseOrderMapper() {
    }

    public static PurchaseOrderResponse toResponse(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getOrderDate(),
                order.getStatus(),
                order.getReceivedDate(),
                order.getUser().getFullName(),
                order.getItems().stream()
                        .map(PurchaseOrderMapper::toItemResponse)
                        .toList());
    }

    private static PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getUnitCost());
    }
}
