package com.power_puff.bms.sale.service;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.dto.SaleItemRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class SaleRollbackIntegrationTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ActivityLogService activityLogService;

    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        entityManager.clear();
    }

    @Test
    void rollsBackSaleAndStockDeductionWhenActivityRecordingFails() {
        User user = userRepository.saveAndFlush(new User(
                "rollback-" + UUID.randomUUID(), "test-hash",
                "Rollback Staff", UserRole.STAFF));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "ROLLBACK-" + UUID.randomUUID(), "Rollback Test Book", null,
                new BigDecimal("40.00"), 5, 1));
        productId = product.getId();

        long salesBefore = saleRepository.count();
        doThrow(new IllegalStateException("Audit storage unavailable"))
                .when(activityLogService)
                .record(eq(user), eq("SALE"), anyString());

        assertThrows(IllegalStateException.class, () -> saleService.create(
                new SaleRequest(null, List.of(new SaleItemRequest(productId, 2))),
                user.getUsername()));

        entityManager.clear();

        assertEquals(5, productRepository.findById(productId)
                .orElseThrow().getQuantity());
        assertEquals(salesBefore, saleRepository.count());
    }
}
