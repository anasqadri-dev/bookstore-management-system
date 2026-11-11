package com.power_puff.bms.schoolorder.service;

import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.schoolorder.dto.SchoolQuotationItemResponse;
import com.power_puff.bms.schoolorder.dto.SchoolQuotationResponse;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SchoolQuotationService {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final SchoolOrderRepository schoolOrderRepository;

    public SchoolQuotationService(SchoolOrderRepository schoolOrderRepository) {
        this.schoolOrderRepository = schoolOrderRepository;
    }

    public SchoolQuotationResponse generate(Integer schoolOrderId) {
        SchoolOrder order = schoolOrderRepository.findById(schoolOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School order with id " + schoolOrderId + " was not found."));

        List<SchoolQuotationItemResponse> items = order.getItems().stream()
                .map(this::toQuotationItem)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(SchoolQuotationItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return new SchoolQuotationResponse(
                order.getId(),
                order.getSchool().getId(),
                order.getSchool().getName(),
                order.getOrderDate(),
                order.getStatus(),
                order.getUser().getFullName(),
                items,
                totalAmount);
    }

    private SchoolQuotationItemResponse toQuotationItem(SchoolOrderItem item) {
        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return new SchoolQuotationItemResponse(
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal);
    }
}
