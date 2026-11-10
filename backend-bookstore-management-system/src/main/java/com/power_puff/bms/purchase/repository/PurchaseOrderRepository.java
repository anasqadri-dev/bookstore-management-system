package com.power_puff.bms.purchase.repository;

import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    List<PurchaseOrder> findAllBySupplierIdOrderByOrderDateDesc(Integer supplierId);

    List<PurchaseOrder> findAllByStatusOrderByOrderDateDesc(PurchaseOrderStatus status);
}
