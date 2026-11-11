package com.power_puff.bms.supplier.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.supplier.dto.SupplierRequest;
import com.power_puff.bms.supplier.dto.SupplierResponse;
import com.power_puff.bms.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse supplier = supplierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully.", supplier));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getSuppliers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Suppliers retrieved successfully.", supplierService.getAll()));
    }
}
