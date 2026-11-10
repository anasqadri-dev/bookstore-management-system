package com.power_puff.bms.product.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.product.dto.ProductRequest;
import com.power_puff.bms.product.dto.ProductResponse;
import com.power_puff.bms.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<ProductResponse>> search(
            @RequestParam(required = false) String search) {
        return ApiResponse.success("Products retrieved successfully", productService.search(search));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<List<ProductResponse>> lowStock() {
        return ApiResponse.success("Low-stock products retrieved successfully",
                productService.getLowStockProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<ProductResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success("Product retrieved successfully", productService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> update(
            @PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Product updated successfully",
                productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ApiResponse.success("Product deleted successfully");
    }
}
