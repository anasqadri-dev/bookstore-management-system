package com.power_puff.bms.report.service;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.report.dto.InventoryReportResponse;
import com.power_puff.bms.report.dto.SalesReportResponse;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private final List<Integer> saleIds = new ArrayList<>();
    private final List<Integer> productIds = new ArrayList<>();
    private Long userId;

    @AfterEach
    void cleanUp() {
        if (!saleIds.isEmpty()) {
            saleRepository.deleteAllById(saleIds);
        }
        if (!productIds.isEmpty()) {
            productRepository.deleteAllById(productIds);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void reportsSalesWithinInclusiveCalendarDateRangeUsingSavedSaleTotals() {
        String suffix = UUID.randomUUID().toString();
        User user = userRepository.saveAndFlush(new User(
                "report-sales-" + suffix, "test-hash",
                "Report Test Cashier", UserRole.STAFF));
        userId = user.getId();

        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = startDate.plusDays(1);
        ZoneId zone = ZoneId.systemDefault();

        createSale(
                startDate.atStartOfDay(zone).toInstant(),
                "Opening Day Sale", new BigDecimal("12.50"), user);
        createSale(
                endDate.atTime(23, 59, 59).atZone(zone).toInstant(),
                "Closing Day Sale", new BigDecimal("27.50"), user);
        createSale(
                startDate.atStartOfDay(zone).minusNanos(1).toInstant(),
                "Before Range Sale", new BigDecimal("100.00"), user);
        createSale(
                endDate.plusDays(1).atStartOfDay(zone).toInstant(),
                "After Range Sale", new BigDecimal("200.00"), user);

        SalesReportResponse baseline = reportService.getSalesReport(startDate, endDate);
        assertEquals(2, baseline.totalSales());
        assertEquals(0, new BigDecimal("40.00").compareTo(baseline.totalAmount()));
        assertEquals(2, baseline.sales().size());
        assertEquals("Report Test Cashier", baseline.sales().getFirst().cashierName());
    }

    @Test
    void rejectsMissingOrReversedSalesReportDates() {
        LocalDate today = LocalDate.now();

        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSalesReport(null, today));
        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSalesReport(today, null));
        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSalesReport(today, today.minusDays(1)));
    }

    @Test
    void valuesInventoryFromDocumentedRetailPriceAndQuantity() {
        InventoryReportResponse before = reportService.getInventoryReport();
        String suffix = UUID.randomUUID().toString();

        Product regularStock = productRepository.saveAndFlush(new Product(
                "REPORT-A-" + suffix, "Report Regular Stock", null,
                new BigDecimal("25.00"), 4, 1));
        productIds.add(regularStock.getId());

        Product lowStock = productRepository.saveAndFlush(new Product(
                "REPORT-B-" + suffix, "Report Low Stock", null,
                new BigDecimal("10.50"), 1, 2));
        productIds.add(lowStock.getId());

        InventoryReportResponse report = reportService.getInventoryReport();

        assertEquals(before.totalProducts() + 2, report.totalProducts());
        assertEquals(before.totalUnits() + 5, report.totalUnits());
        assertEquals(before.lowStockProducts() + 1, report.lowStockProducts());
        assertEquals(0, before.totalStockValue()
                .add(new BigDecimal("110.50"))
                .compareTo(report.totalStockValue()));

        var regularLine = report.products().stream()
                .filter(item -> item.productId().equals(regularStock.getId()))
                .findFirst()
                .orElseThrow();
        var lowStockLine = report.products().stream()
                .filter(item -> item.productId().equals(lowStock.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("100.00").compareTo(regularLine.stockValue()));
        assertEquals(0, new BigDecimal("10.50").compareTo(lowStockLine.stockValue()));
        assertEquals(true, lowStockLine.lowStock());
    }

    private void createSale(Instant saleDate, String customerName,
                            BigDecimal totalAmount, User user) {
        Sale sale = saleRepository.saveAndFlush(
                new Sale(saleDate, customerName, totalAmount, user));
        saleIds.add(sale.getId());
    }
}
