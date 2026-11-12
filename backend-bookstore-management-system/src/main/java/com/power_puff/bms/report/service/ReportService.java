package com.power_puff.bms.report.service;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.report.dto.InventoryReportItemResponse;
import com.power_puff.bms.report.dto.InventoryReportResponse;
import com.power_puff.bms.report.dto.SalesReportItemResponse;
import com.power_puff.bms.report.dto.SalesReportResponse;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ZoneId businessTimeZone;

    public ReportService(SaleRepository saleRepository,
                         ProductRepository productRepository,
                         @Value("${bms.business-time-zone:Asia/Karachi}") String businessTimeZone) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.businessTimeZone = ZoneId.of(businessTimeZone);
    }

    public SalesReportResponse getSalesReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both start date and end date are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        var startInclusive = startDate.atStartOfDay(businessTimeZone).toInstant();
        var endExclusive = endDate.plusDays(1).atStartOfDay(businessTimeZone).toInstant();

        List<Sale> sales = saleRepository
                .findAllBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(
                        startInclusive, endExclusive);

        List<SalesReportItemResponse> rows = sales.stream()
                .map(this::toSalesReportItem)
                .toList();

        BigDecimal totalAmount = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return new SalesReportResponse(
                startDate, endDate, sales.size(), totalAmount, rows);
    }

    public InventoryReportResponse getInventoryReport() {
        List<Product> products = productRepository.findAllByOrderByTitleAsc();

        List<InventoryReportItemResponse> rows = products.stream()
                .map(this::toInventoryReportItem)
                .toList();

        long totalUnits = products.stream()
                .mapToLong(product -> product.getQuantity().longValue())
                .sum();
        long lowStockProducts = products.stream()
                .filter(Product::isLowStock)
                .count();

        BigDecimal totalStockValue = rows.stream()
                .map(InventoryReportItemResponse::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return new InventoryReportResponse(
                products.size(),
                totalUnits,
                lowStockProducts,
                totalStockValue,
                rows);
    }

    private SalesReportItemResponse toSalesReportItem(Sale sale) {
        return new SalesReportItemResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getCustomerName(),
                sale.getUser().getFullName(),
                sale.getTotalAmount());
    }

    private InventoryReportItemResponse toInventoryReportItem(Product product) {
        BigDecimal stockValue = product.getPrice()
                .multiply(BigDecimal.valueOf(product.getQuantity()))
                .setScale(CURRENCY_SCALE, ROUNDING_MODE);

        return new InventoryReportItemResponse(
                product.getId(),
                product.getSku(),
                product.getTitle(),
                product.getAuthor(),
                product.getPrice(),
                product.getQuantity(),
                product.getMinStock(),
                product.isLowStock(),
                stockValue);
    }
}
