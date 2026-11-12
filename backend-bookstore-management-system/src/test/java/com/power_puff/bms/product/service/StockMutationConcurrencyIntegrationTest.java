package com.power_puff.bms.product.service;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.sale.service.SaleService;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/bms-stock-concurrency.db",
        "spring.datasource.hikari.maximum-pool-size=4",
        "spring.datasource.hikari.minimum-idle=1"
})
@ActiveProfiles("test")
class StockMutationConcurrencyIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbcTemplate.update(
                    "DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE user_id = ?)",
                    userId);
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM sales WHERE user_id = ?", userId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void twoConcurrentCheckoutsCannotSellTheSameFinalUnit() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User cashier = userRepository.saveAndFlush(new User(
                "stock-race-" + suffix,
                "test-hash",
                "Concurrent Checkout Cashier",
                UserRole.STAFF));
        userId = cashier.getId();

        Product book = productRepository.saveAndFlush(new Product(
                "STOCK-RACE-" + suffix,
                "Last Copy Book",
                null,
                new BigDecimal("12.50"),
                1,
                0));
        productId = book.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() ->
                    attemptCheckout(cashier.getUsername(), productId, ready, start));
            Future<Integer> second = executor.submit(() ->
                    attemptCheckout(cashier.getUsername(), productId, ready, start));

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "Both checkout workers should reach the start barrier.");
            start.countDown();

            int firstSaleId = first.get(20, TimeUnit.SECONDS);
            int secondSaleId = second.get(20, TimeUnit.SECONDS);
            int successfulSales = (firstSaleId > 0 ? 1 : 0)
                    + (secondSaleId > 0 ? 1 : 0);

            assertEquals(1, successfulSales,
                    "Exactly one checkout may consume the final available unit.");
        }

        assertEquals(0, productRepository.findById(productId).orElseThrow().getQuantity());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE user_id = ?", Long.class, userId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sale_items WHERE sale_id IN " +
                        "(SELECT id FROM sales WHERE user_id = ?)", Long.class, userId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE user_id = ? AND action = 'SALE'",
                Long.class, userId));
    }

    @Test
    @Transactional
    void conditionalStockMutationsEnforceAvailabilityAndIntegerBoundaries() {
        String suffix = UUID.randomUUID().toString();
        Product book = productRepository.saveAndFlush(new Product(
                "STOCK-BOUNDARY-" + suffix,
                "Stock Boundary Book",
                null,
                new BigDecimal("5.00"),
                1,
                0));
        productId = book.getId();

        assertEquals(1, productRepository.decrementStockIfAvailable(productId, 1));
        assertEquals(0, productRepository.decrementStockIfAvailable(productId, 1));

        assertEquals(1, productRepository.incrementStockIfWithinRange(
                productId, 2, Integer.MAX_VALUE - 2));
        assertEquals(0, productRepository.incrementStockIfWithinRange(
                productId, Integer.MAX_VALUE, 0));

        entityManager.clear();
        assertEquals(2, productRepository.findById(productId).orElseThrow().getQuantity());
    }

    private Integer attemptCheckout(String username,
                                    Integer requestedProductId,
                                    CountDownLatch ready,
                                    CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Checkout start barrier timed out.");
        }

        try {
            SaleReceiptResponse receipt = saleService.create(
                    new SaleRequest("Concurrent Customer",
                            List.of(new SaleItemRequest(requestedProductId, 1))),
                    username);
            return receipt.id();
        } catch (IllegalArgumentException | DataAccessException | DuplicateResourceException conflict) {
            // Stock validation or a competing database update may reject the losing checkout.
            return -1;
        }
    }
}
