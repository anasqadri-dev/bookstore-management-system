package com.power_puff.bms.purchase.repository;

import com.power_puff.bms.purchase.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {

    List<PurchaseOrderItem> findAllByPurchaseOrderIdOrderByIdAsc(Integer purchaseOrderId);
}
