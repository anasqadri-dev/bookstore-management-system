package com.power_puff.bms.sale.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.service.SaleService;
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

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleReceiptResponse>> createSale(
            @Valid @RequestBody SaleRequest request,
            Principal principal) {
        SaleReceiptResponse receipt = saleService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale completed successfully.", receipt));
    }

    @GetMapping("/{saleId}/receipt")
    public ResponseEntity<ApiResponse<SaleReceiptResponse>> getReceipt(
            @PathVariable Integer saleId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sale receipt retrieved successfully.",
                saleService.getReceipt(saleId)));
    }
}
