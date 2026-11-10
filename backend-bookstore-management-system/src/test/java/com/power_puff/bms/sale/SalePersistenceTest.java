package com.power_puff.bms.sale;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.entity.SaleItem;
import com.power_puff.bms.sale.repository.SaleItemRepository;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class SalePersistenceTest {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Integer saleId;
    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        if (saleId != null) {
            saleItemRepository.deleteAll(saleItemRepository.findAllBySaleId(saleId));
            saleRepository.deleteById(saleId);
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
    void persistsSaleAndPreservesHistoricalUnitPriceAfterProductPriceChange() {
        String suffix = UUID.randomUUID().toString();

        User user = userRepository.saveAndFlush(new User(
                "sale-test-" + suffix, "test-hash", "Sales Test User", UserRole.STAFF));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "SALE-" + suffix, "Sale Test Book", null,
                new BigDecimal("15.00"), 10, 2));
        productId = product.getId();

        Sale sale = new Sale(
                Instant.parse("2026-11-10T10:00:00Z"),
                null,
                new BigDecimal("30.00"),
                user);
        sale.addItem(new SaleItem(
                product, 2, new BigDecimal("15.00"), new BigDecimal("30.00")));

        Sale savedSale = saleRepository.saveAndFlush(sale);
        saleId = savedSale.getId();

        assertNotNull(saleId);
        assertEquals(new BigDecimal("30.00"), savedSale.getTotalAmount());
        assertNotNull(savedSale.getSaleDate());

        product.setPrice(new BigDecimal("20.00"));
        productRepository.saveAndFlush(product);
        entityManager.clear();

        SaleItem savedItem = saleItemRepository.findAllBySaleId(saleId).getFirst();
        assertEquals(2, savedItem.getQuantity());
        assertEquals(new BigDecimal("15.00"), savedItem.getUnitPrice());
        assertEquals(new BigDecimal("30.00"), savedItem.getSubtotal());
    }
}
