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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SaleServiceIntegrationTest {

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

    private final List<Integer> saleIds = new ArrayList<>();
    private final List<Integer> productIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (Integer saleId : saleIds) {
            jdbcTemplate.update("DELETE FROM sale_items WHERE sale_id = ?", saleId);
            saleRepository.deleteById(saleId);
        }
        for (Integer productId : productIds) {
            productRepository.deleteById(productId);
        }
        for (Long userId : userIds) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
        entityManager.clear();
    }

    @Test
    void createsSaleDeductsAggregatedQuantitiesAndReturnsReceipt() {
        User user = createUser();
        Product notebook = createProduct("CHECKOUT-A", "Notebook",
                new BigDecimal("15.00"), 10);
        Product textbook = createProduct("CHECKOUT-B", "Textbook",
                new BigDecimal("30.00"), 4);

        SaleRequest request = new SaleRequest("Walk-in Customer", List.of(
                new SaleItemRequest(notebook.getId(), 2),
                new SaleItemRequest(textbook.getId(), 1),
                new SaleItemRequest(notebook.getId(), 1)));

        SaleReceiptResponse receipt = saleService.create(request, user.getUsername());
        saleIds.add(receipt.id());

        assertEquals(0, new BigDecimal("75.00").compareTo(receipt.totalAmount()));
        assertEquals(2, receipt.items().size());
        assertEquals("Checkout Staff", receipt.cashierName());
        assertEquals(7, productRepository.findById(notebook.getId()).orElseThrow().getQuantity());
        assertEquals(3, productRepository.findById(textbook.getId()).orElseThrow().getQuantity());

        long saleLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE user_id = ? AND action = 'SALE'",
                Long.class, user.getId());
        assertEquals(1L, saleLogCount);

        Product repricedNotebook = productRepository.findById(notebook.getId()).orElseThrow();
        repricedNotebook.setPrice(new BigDecimal("20.00"));
        productRepository.saveAndFlush(repricedNotebook);
        entityManager.clear();

        SaleReceiptResponse retrievedReceipt = saleService.getReceipt(receipt.id());
        assertEquals(0, new BigDecimal("15.00").compareTo(
                retrievedReceipt.items().getFirst().unitPrice()));
        assertEquals(receipt.id(), retrievedReceipt.id());
    }

    @Test
    void doesNotDeductAnyStockWhenOneRequestedProductHasInsufficientQuantity() {
        User user = createUser();
        Product availableProduct = createProduct("CHECKOUT-C", "Available Book",
                new BigDecimal("10.00"), 5);
        Product limitedProduct = createProduct("CHECKOUT-D", "Limited Book",
                new BigDecimal("20.00"), 1);
        long salesBefore = saleRepository.count();

        SaleRequest request = new SaleRequest(null, List.of(
                new SaleItemRequest(availableProduct.getId(), 2),
                new SaleItemRequest(limitedProduct.getId(), 2)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> saleService.create(request, user.getUsername()));

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertEquals(5, productRepository.findById(availableProduct.getId())
                .orElseThrow().getQuantity());
        assertEquals(1, productRepository.findById(limitedProduct.getId())
                .orElseThrow().getQuantity());
        assertEquals(salesBefore, saleRepository.count());
    }

    @Test
    void rejectsSaleWithoutItems() {
        assertThrows(IllegalArgumentException.class,
                () -> saleService.create(new SaleRequest(null, List.of()), "unused"));
    }

    private User createUser() {
        User user = userRepository.saveAndFlush(new User(
                "checkout-" + UUID.randomUUID(), "test-hash",
                "Checkout Staff", UserRole.STAFF));
        userIds.add(user.getId());
        return user;
    }

    private Product createProduct(String sku, String title, BigDecimal price, int quantity) {
        Product product = productRepository.saveAndFlush(new Product(
                sku + "-" + UUID.randomUUID(), title, null, price, quantity, 1));
        productIds.add(product.getId());
        return product;
    }
}
