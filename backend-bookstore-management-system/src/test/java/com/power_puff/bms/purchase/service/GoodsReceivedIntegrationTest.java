package com.power_puff.bms.purchase.service;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.dto.PurchaseOrderItemRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import com.power_puff.bms.purchase.repository.PurchaseOrderRepository;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class GoodsReceivedIntegrationTest {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Integer purchaseOrderId;
    private Integer supplierId;
    private Integer productId;
    private Long userId;
    private boolean stockFailureTriggerCreated;

    @AfterEach
    void cleanUp() {
        if (stockFailureTriggerCreated) {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS purchasing_test_fail_stock_update");
        }
        entityManager.clear();
        if (purchaseOrderId != null) {
            jdbcTemplate.update(
                    "DELETE FROM purchase_order_items WHERE purchase_order_id = ?",
                    purchaseOrderId);
            purchaseOrderRepository.deleteById(purchaseOrderId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (supplierId != null) {
            supplierRepository.deleteById(supplierId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        entityManager.clear();
    }

    @Test
    void receivesGoodsUpdatesStockAndRecordsReceivedDate() {
        String suffix = UUID.randomUUID().toString();
        createSupplierUserAndProduct(suffix, 10);

        PurchaseOrderResponse created = createPurchaseOrder(List.of(
                new PurchaseOrderItemRequest(productId, 3, new BigDecimal("55.00")),
                new PurchaseOrderItemRequest(productId, 4, new BigDecimal("57.50"))));

        purchaseOrderId = created.id();
        assertEquals(PurchaseOrderStatus.ORDERED, created.status());
        assertEquals(10, productRepository.findById(productId).orElseThrow().getQuantity());

        PurchaseOrderResponse received = purchaseOrderService.receiveGoods(purchaseOrderId);

        assertEquals(PurchaseOrderStatus.RECEIVED, received.status());
        assertNotNull(received.receivedDate());
        assertEquals(17, productRepository.findById(productId).orElseThrow().getQuantity());

        entityManager.clear();
        var saved = purchaseOrderRepository.findById(purchaseOrderId).orElseThrow();
        assertEquals(PurchaseOrderStatus.RECEIVED, saved.getStatus());
        assertNotNull(saved.getReceivedDate());
    }

    @Test
    void rejectsDuplicateReceiptWithoutIncreasingStockAgain() {
        String suffix = UUID.randomUUID().toString();
        createSupplierUserAndProduct(suffix, 5);

        PurchaseOrderResponse created = createPurchaseOrder(List.of(
                new PurchaseOrderItemRequest(productId, 6, new BigDecimal("30.00"))));
        purchaseOrderId = created.id();

        purchaseOrderService.receiveGoods(purchaseOrderId);
        assertEquals(11, productRepository.findById(productId).orElseThrow().getQuantity());

        assertThrows(DuplicateResourceException.class,
                () -> purchaseOrderService.receiveGoods(purchaseOrderId));

        entityManager.clear();
        assertEquals(11, productRepository.findById(productId).orElseThrow().getQuantity());
        var saved = purchaseOrderRepository.findById(purchaseOrderId).orElseThrow();
        assertEquals(PurchaseOrderStatus.RECEIVED, saved.getStatus());
        assertNotNull(saved.getReceivedDate());
    }

    @Test
    void rollsBackReceivedStatusAndStockWhenQuantityWouldOverflow() {
        String suffix = UUID.randomUUID().toString();
        createSupplierUserAndProduct(suffix, Integer.MAX_VALUE - 1);

        PurchaseOrderResponse created = createPurchaseOrder(List.of(
                new PurchaseOrderItemRequest(productId, 2, new BigDecimal("1.00"))));
        purchaseOrderId = created.id();

        assertThrows(IllegalArgumentException.class,
                () -> purchaseOrderService.receiveGoods(purchaseOrderId));

        entityManager.clear();
        assertEquals(Integer.MAX_VALUE - 1,
                productRepository.findById(productId).orElseThrow().getQuantity());
        var saved = purchaseOrderRepository.findById(purchaseOrderId).orElseThrow();
        assertEquals(PurchaseOrderStatus.ORDERED, saved.getStatus());
        assertEquals(null, saved.getReceivedDate());
    }

    @Test
    void rollsBackReceivedStatusAndStockWhenStockPersistenceFails() {
        String suffix = UUID.randomUUID().toString();
        createSupplierUserAndProduct(suffix, 8);

        PurchaseOrderResponse created = createPurchaseOrder(List.of(
                new PurchaseOrderItemRequest(productId, 3, new BigDecimal("20.00"))));
        purchaseOrderId = created.id();

        jdbcTemplate.execute("""
                CREATE TRIGGER purchasing_test_fail_stock_update
                BEFORE UPDATE OF quantity ON products
                WHEN OLD.id = %d
                BEGIN
                    SELECT RAISE(ABORT, 'simulated inventory write failure');
                END
                """.formatted(productId));
        stockFailureTriggerCreated = true;

        assertThrows(DuplicateResourceException.class,
                () -> purchaseOrderService.receiveGoods(purchaseOrderId));

        entityManager.clear();
        assertEquals(8, productRepository.findById(productId).orElseThrow().getQuantity());
        var saved = purchaseOrderRepository.findById(purchaseOrderId).orElseThrow();
        assertEquals(PurchaseOrderStatus.ORDERED, saved.getStatus());
        assertEquals(null, saved.getReceivedDate());
    }

    @Test
    void rejectsReceiptForUnknownPurchaseOrder() {
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.receiveGoods(999999));
    }

    private void createSupplierUserAndProduct(String suffix, int stock) {
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Goods Received Supplier " + suffix, null, null));
        supplierId = supplier.getId();

        User user = userRepository.saveAndFlush(new User(
                "goods-received-" + suffix, "test-hash",
                "Goods Received Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "GOODS-RECEIVED-" + suffix, "Goods Received Book", null,
                new BigDecimal("100.00"), stock, 0));
        productId = product.getId();
    }

    private PurchaseOrderResponse createPurchaseOrder(
            List<PurchaseOrderItemRequest> items) {
        return purchaseOrderService.create(
                new PurchaseOrderRequest(supplierId, items),
                userRepository.findById(userId).orElseThrow().getUsername());
    }
}
