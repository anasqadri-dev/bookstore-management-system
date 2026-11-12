package com.power_puff.bms.dashboard.service;

import com.power_puff.bms.dashboard.dto.DashboardResponse;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void returnsDashboardSummaryFromPersistedData() {
        DashboardResponse before = dashboardService.getDashboard();

        String suffix = UUID.randomUUID().toString();
        Product lowStock = productRepository.saveAndFlush(new Product(
                "DASH-LOW-" + suffix, "Dashboard Low Stock", null,
                new BigDecimal("10.00"), 2, 2));
        Product healthyStock = productRepository.saveAndFlush(new Product(
                "DASH-OK-" + suffix, "Dashboard Healthy Stock", null,
                new BigDecimal("20.00"), 8, 2));
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Dashboard Supplier " + suffix, null, null));
        User cashier = userRepository.saveAndFlush(new User(
                "dashboard-" + suffix, "already-encoded", "Dashboard Cashier", UserRole.STAFF));

        Instant now = Instant.now();
        Sale oldestToday = saleRepository.saveAndFlush(new Sale(
                now.minusSeconds(20), "Earlier Customer",
                new BigDecimal("25.50"), cashier));
        Sale newestToday = saleRepository.saveAndFlush(new Sale(
                now, "Recent Customer",
                new BigDecimal("40.25"), cashier));

        DashboardResponse dashboard = dashboardService.getDashboard();

        assertEquals(before.totalBooks() + 2, dashboard.totalBooks());
        assertEquals(before.suppliers() + 1, dashboard.suppliers());
        assertEquals(before.lowStock() + 1, dashboard.lowStock());
        assertEquals(before.todaySales().add(new BigDecimal("65.75")),
                dashboard.todaySales());

        assertFalse(dashboard.recentSales().isEmpty());
        assertEquals(newestToday.getId(), dashboard.recentSales().get(0).saleId());
        assertTrue(dashboard.recentSales().size() <= 5);
        assertTrue(dashboard.recentSales().stream()
                .anyMatch(item -> item.saleId().equals(oldestToday.getId())));
        assertTrue(dashboard.recentSales().stream()
                .noneMatch(item -> item.saleId() == null));
    }

    @Test
    void limitsRecentSalesToFiveAndKeepsNewestFirst() {
        String suffix = UUID.randomUUID().toString();
        User cashier = userRepository.saveAndFlush(new User(
                "dashboard-order-" + suffix, "already-encoded",
                "Dashboard Ordering Cashier", UserRole.STAFF));

        Instant base = Instant.now().minusSeconds(60);
        List<Sale> sales = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            sales.add(saleRepository.saveAndFlush(new Sale(
                    base.plusSeconds(index),
                    "Customer " + index,
                    new BigDecimal("1.00"),
                    cashier)));
        }

        DashboardResponse dashboard = dashboardService.getDashboard();

        assertEquals(5, dashboard.recentSales().size());
        assertEquals(sales.get(6).getId(), dashboard.recentSales().get(0).saleId());
        assertEquals(sales.get(2).getId(), dashboard.recentSales().get(4).saleId());
    }
}
