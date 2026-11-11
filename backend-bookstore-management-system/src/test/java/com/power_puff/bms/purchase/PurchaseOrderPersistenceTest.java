package com.power_puff.bms.purchase;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderItem;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PurchaseOrderPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsPurchaseOrderAndItemsWithOrderedStatusAndUnitCostSnapshot() {
        String suffix = UUID.randomUUID().toString();

        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Purchase Supplier " + suffix, "03001234567", "Wholesale Market"));
        User user = userRepository.saveAndFlush(new User(
                "purchase-order-" + suffix, "test-hash",
                "Purchasing Admin", UserRole.ADMIN));
        Product product = productRepository.saveAndFlush(new Product(
                "PURCHASE-" + suffix, "Purchased Book", null,
                new BigDecimal("80.00"), 10, 2));

        PurchaseOrder order = new PurchaseOrder(supplier, user);
        order.addItem(new PurchaseOrderItem(
                product, 6, new BigDecimal("55.00")));

        entityManager.persist(order);
        entityManager.flush();

        assertNotNull(order.getId());
        assertEquals(PurchaseOrderStatus.ORDERED, order.getStatus());
        assertNotNull(order.getOrderDate());
        assertNull(order.getReceivedDate());
        assertEquals(1, order.getItems().size());

        product.setPrice(new BigDecimal("95.00"));
        productRepository.saveAndFlush(product);
        entityManager.clear();

        PurchaseOrder saved = entityManager.find(PurchaseOrder.class, order.getId());
        PurchaseOrderItem savedItem = saved.getItems().getFirst();

        assertEquals("Purchase Supplier " + suffix, saved.getSupplier().getName());
        assertEquals("Purchasing Admin", saved.getUser().getFullName());
        assertEquals(6, savedItem.getQuantity());
        assertEquals(0, new BigDecimal("55.00").compareTo(savedItem.getUnitCost()));
        assertEquals(0, new BigDecimal("95.00").compareTo(
                savedItem.getProduct().getPrice()));
    }

    @Test
    void allowsReceivedStatusWithReceivedDate() {
        String suffix = UUID.randomUUID().toString();
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Received Supplier " + suffix, null, null));
        User user = userRepository.saveAndFlush(new User(
                "purchase-received-" + suffix, "test-hash",
                "Receiving Admin", UserRole.ADMIN));
        LocalDate receivedDate = LocalDate.of(2026, 11, 11);

        PurchaseOrder order = new PurchaseOrder(
                supplier, LocalDate.of(2026, 11, 10),
                PurchaseOrderStatus.RECEIVED, receivedDate, user);
        entityManager.persist(order);
        entityManager.flush();

        assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
        assertEquals(receivedDate, order.getReceivedDate());
    }
}
