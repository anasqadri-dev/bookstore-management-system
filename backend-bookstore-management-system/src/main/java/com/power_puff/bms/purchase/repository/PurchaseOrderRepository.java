package com.power_puff.bms.purchase.repository;

import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findAllByOrderByOrderDateDesc();

    List<PurchaseOrder> findAllBySupplierIdOrderByOrderDateDesc(Integer supplierId);

    List<PurchaseOrder> findAllByStatusOrderByOrderDateDesc(PurchaseOrderStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PurchaseOrder purchaseOrder
               set purchaseOrder.status = :receivedStatus,
                   purchaseOrder.receivedDate = :receivedDate
             where purchaseOrder.id = :purchaseOrderId
               and purchaseOrder.status = :orderedStatus
            """)
    int markReceivedIfOrdered(
            @Param("purchaseOrderId") Integer purchaseOrderId,
            @Param("orderedStatus") PurchaseOrderStatus orderedStatus,
            @Param("receivedStatus") PurchaseOrderStatus receivedStatus,
            @Param("receivedDate") LocalDate receivedDate);
}
