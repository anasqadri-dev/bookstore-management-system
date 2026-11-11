package com.power_puff.bms.purchase;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderItem;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import com.power_puff.bms.purchase.repository.PurchaseOrderItemRepository;
import com.power_puff.bms.purchase.repository.PurchaseOrderRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PurchaseOrderRepositoryTest {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsPurchaseOrdersBySupplierStatusAndOrderDateAndFindsTheirItems() {
        String suffix = UUID.randomUUID().toString();

        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Repository Supplier " + suffix, null, null));
        User user = userRepository.saveAndFlush(new User(
                "purchase-repo-" + suffix, "test-hash",
                "Purchasing Admin", UserRole.ADMIN));
        Product product = productRepository.saveAndFlush(new Product(
                "PURCHASE-REPO-" + suffix, "Repository Purchase Book", null,
                new BigDecimal("100.00"), 10, 1));

        PurchaseOrder order = new PurchaseOrder(supplier, user);
        order.addItem(new PurchaseOrderItem(product, 4, new BigDecimal("60.00")));
        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(order);

        assertEquals(1, purchaseOrderRepository
                .findAllBySupplierIdOrderByOrderDateDesc(supplier.getId()).size());
        assertEquals(saved.getId(),
                purchaseOrderRepository.findAllByStatusOrderByOrderDateDesc(
                        PurchaseOrderStatus.ORDERED).stream()
                        .filter(row -> row.getId().equals(saved.getId()))
                        .findFirst().orElseThrow().getId());
        assertEquals(saved.getId(),
                purchaseOrderRepository.findAllByOrderByOrderDateDesc().stream()
                        .filter(row -> row.getId().equals(saved.getId()))
                        .findFirst().orElseThrow().getId());

        var items = purchaseOrderItemRepository
                .findAllByPurchaseOrderIdOrderByIdAsc(saved.getId());
        assertEquals(1, items.size());
        assertEquals(4, items.getFirst().getQuantity());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                items.getFirst().getUnitCost()));
    }
}
