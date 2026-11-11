package com.power_puff.bms.purchase.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.purchase.dto.PurchaseOrderRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@PreAuthorize("hasRole('ADMIN')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            Principal principal) {
        PurchaseOrderResponse purchaseOrder = purchaseOrderService.create(
                request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Purchase order created successfully.", purchaseOrder));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getPurchaseOrders() {
        return ResponseEntity.ok(ApiResponse.success(
                "Purchase orders retrieved successfully.",
                purchaseOrderService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrder(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Purchase order retrieved successfully.",
                purchaseOrderService.getById(id)));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveGoods(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Goods received successfully.",
                purchaseOrderService.receiveGoods(id)));
    }
}
