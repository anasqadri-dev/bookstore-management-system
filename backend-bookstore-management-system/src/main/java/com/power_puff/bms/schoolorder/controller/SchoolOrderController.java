package com.power_puff.bms.schoolorder.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.schoolorder.dto.SchoolOrderRequest;
import com.power_puff.bms.schoolorder.dto.SchoolQuotationResponse;
import com.power_puff.bms.schoolorder.service.SchoolQuotationService;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.service.SchoolOrderService;
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
@RequestMapping("/api/school-orders")
@PreAuthorize("hasRole('ADMIN')")
public class SchoolOrderController {

    private final SchoolOrderService schoolOrderService;
    private final SchoolQuotationService schoolQuotationService;

    public SchoolOrderController(SchoolOrderService schoolOrderService,
                                 SchoolQuotationService schoolQuotationService) {
        this.schoolOrderService = schoolOrderService;
        this.schoolQuotationService = schoolQuotationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SchoolOrderResponse>> createOrder(
            @Valid @RequestBody SchoolOrderRequest request,
            Principal principal) {
        SchoolOrderResponse order = schoolOrderService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("School order created successfully.", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolOrderResponse>>> getOrders() {
        return ResponseEntity.ok(ApiResponse.success(
                "School orders retrieved successfully.",
                schoolOrderService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolOrderResponse>> getOrder(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                "School order retrieved successfully.",
                schoolOrderService.getById(id)));
    }

    @GetMapping("/{id}/quotation")
    public ResponseEntity<ApiResponse<SchoolQuotationResponse>> getQuotation(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(
                "School order quotation generated successfully.",
                schoolQuotationService.generate(id)));
    }
}
