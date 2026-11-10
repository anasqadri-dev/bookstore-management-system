package com.power_puff.bms.sale.service;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SalePerformanceTest {

    private static final long MAX_CONFIRM_TO_RECEIPT_MILLIS = 5_000;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

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
        entityManager.clear();
        if (userId != null) {
            jdbcTemplate.update(
                    "DELETE FROM activity_logs WHERE user_id = ?", userId);
            jdbcTemplate.update(
                    "DELETE FROM sale_items WHERE sale_id IN " +
                            "(SELECT id FROM sales WHERE user_id = ?)", userId);
            jdbcTemplate.update("DELETE FROM sales WHERE user_id = ?", userId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        entityManager.clear();
    }

    @Test
    void confirmToReceiptCompletesWithinFiveSecondsAfterWarmup() {
        User user = userRepository.saveAndFlush(new User(
                "performance-" + UUID.randomUUID(), "test-hash",
                "Performance Staff", UserRole.STAFF));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "PERFORMANCE-" + UUID.randomUUID(), "Performance Test Book", null,
                new BigDecimal("50.00"), 3, 0));
        productId = product.getId();

        SaleRequest request = new SaleRequest(null,
                List.of(new SaleItemRequest(productId, 1)));

        SaleReceiptResponse warmup = saleService.create(request, user.getUsername());
        saleService.getReceipt(warmup.id());

        long startedAt = System.nanoTime();
        SaleReceiptResponse confirmedSale = saleService.create(request, user.getUsername());
        SaleReceiptResponse receipt = saleService.getReceipt(confirmedSale.id());
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertNotNull(receipt.id());
        assertTrue(elapsedMillis <= MAX_CONFIRM_TO_RECEIPT_MILLIS,
                "Confirm-to-receipt took " + elapsedMillis
                        + " ms; the NFR-02 limit is 5,000 ms.");
    }
}
