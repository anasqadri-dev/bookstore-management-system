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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class SaleReliabilityTest {

    private static final int SALE_COUNT = 200;

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
    void completesTwoHundredSalesWithoutErrorsOrStockMismatch() {
        User user = userRepository.saveAndFlush(new User(
                "reliability-" + UUID.randomUUID(), "test-hash",
                "Reliability Staff", UserRole.STAFF));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "RELIABILITY-" + UUID.randomUUID(), "Reliability Test Book", null,
                new BigDecimal("1.25"), SALE_COUNT, 0));
        productId = product.getId();

        long salesBefore = saleRepository.count();
        BigDecimal expectedTotal = new BigDecimal("1.25")
                .multiply(BigDecimal.valueOf(SALE_COUNT));

        for (int index = 0; index < SALE_COUNT; index++) {
            SaleReceiptResponse receipt = saleService.create(
                    new SaleRequest(null,
                            List.of(new SaleItemRequest(productId, 1))),
                    user.getUsername());

            assertNotNull(receipt.id(), "Sale " + (index + 1) + " was not persisted.");
            assertEquals(0, new BigDecimal("1.25").compareTo(receipt.totalAmount()));
        }

        entityManager.clear();

        assertEquals(salesBefore + SALE_COUNT, saleRepository.count());
        assertEquals(0, productRepository.findById(productId)
                .orElseThrow().getQuantity());
        Long saleLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE user_id = ? AND action = 'SALE'",
                Long.class, userId);
        assertEquals((long) SALE_COUNT, saleLogCount);
        BigDecimal savedTotal = jdbcTemplate.queryForObject(
                "SELECT SUM(total_amount) FROM sales WHERE user_id = ?",
                BigDecimal.class, userId);
        assertEquals(0, expectedTotal.compareTo(savedTotal));
    }
}
