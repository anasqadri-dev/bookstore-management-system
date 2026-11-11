package com.power_puff.bms.purchase.service;

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
class PurchaseOrderServiceIntegrationTest {

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

    @AfterEach
    void cleanUp() {
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
    void createsOrderedPurchaseOrderWithSeparateUnitCostSnapshots() {
        String suffix = UUID.randomUUID().toString();
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Create Purchase Supplier " + suffix, null, null));
        supplierId = supplier.getId();

        User user = userRepository.saveAndFlush(new User(
                "create-purchase-" + suffix, "test-hash",
                "Purchasing Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "CREATE-PURCHASE-" + suffix, "Purchase Test Book", null,
                new BigDecimal("100.00"), 25, 3));
        productId = product.getId();

        PurchaseOrderRequest request = new PurchaseOrderRequest(
                supplierId,
                List.of(
                        new PurchaseOrderItemRequest(productId, 2, new BigDecimal("60.00")),
                        new PurchaseOrderItemRequest(productId, 3, new BigDecimal("62.50"))));

        PurchaseOrderResponse response = purchaseOrderService.create(
                request, user.getUsername());
        purchaseOrderId = response.id();

        assertNotNull(purchaseOrderId);
        assertEquals(supplierId, response.supplierId());
        assertEquals("Create Purchase Supplier " + suffix, response.supplierName());
        assertEquals(PurchaseOrderStatus.ORDERED, response.status());
        assertEquals("Purchasing Admin", response.createdBy());
        assertEquals(2, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(3, response.items().get(1).quantity());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                response.items().get(0).unitCost()));
        assertEquals(0, new BigDecimal("62.50").compareTo(
                response.items().get(1).unitCost()));

        assertEquals(25, productRepository.findById(productId).orElseThrow().getQuantity());

        Product repriced = productRepository.findById(productId).orElseThrow();
        repriced.setPrice(new BigDecimal("120.00"));
        productRepository.saveAndFlush(repriced);
        entityManager.clear();

        PurchaseOrderResponse saved = purchaseOrderService.getById(purchaseOrderId);
        assertEquals(0, new BigDecimal("60.00").compareTo(
                saved.items().get(0).unitCost()));
        assertEquals(0, new BigDecimal("62.50").compareTo(
                saved.items().get(1).unitCost()));
    }

    @Test
    void rejectsUnknownSupplier() {
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.create(
                        new PurchaseOrderRequest(999999,
                                List.of(new PurchaseOrderItemRequest(
                                        1, 1, new BigDecimal("10.00")))),
                        "admin"));
    }

    @Test
    void rejectsUnknownProductWithoutSavingAnOrder() {
        String suffix = UUID.randomUUID().toString();
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Missing Product Supplier " + suffix, null, null));
        supplierId = supplier.getId();
        User user = userRepository.saveAndFlush(new User(
                "missing-purchase-product-" + suffix, "test-hash",
                "Purchasing Admin", UserRole.ADMIN));
        userId = user.getId();
        Product validProduct = productRepository.saveAndFlush(new Product(
                "VALID-PURCHASE-" + suffix, "Valid Purchase Book", null,
                new BigDecimal("20.00"), 5, 1));
        productId = validProduct.getId();

        long before = purchaseOrderRepository.count();
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                supplierId,
                List.of(
                        new PurchaseOrderItemRequest(productId, 1, new BigDecimal("10.00")),
                        new PurchaseOrderItemRequest(999999, 1, new BigDecimal("5.00"))));

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.create(request, user.getUsername()));
        assertEquals(before, purchaseOrderRepository.count());
        assertEquals(5, productRepository.findById(productId).orElseThrow().getQuantity());
    }

    @Test
    void rejectsPurchaseOrderWithoutItems() {
        assertThrows(IllegalArgumentException.class,
                () -> purchaseOrderService.create(
                        new PurchaseOrderRequest(1, List.of()), "admin"));
    }
}
