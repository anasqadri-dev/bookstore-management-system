package com.power_puff.bms.purchase.controller;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import com.power_puff.bms.purchase.repository.PurchaseOrderRepository;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseOrderControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Integer purchaseOrderId;
    private Integer supplierId;
    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        if (purchaseOrderId != null) {
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
    }

    @Test
    @WithMockUser(username = "purchase-controller-admin", roles = "ADMIN")
    void adminCanCreateListViewAndReceivePurchaseOrder() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Controller Supplier " + suffix, "03001234567", "Wholesale Market"));
        supplierId = supplier.getId();

        User user = userRepository.saveAndFlush(new User(
                "purchase-controller-admin", "test-hash",
                "Purchase Controller Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "PURCHASE-CONTROLLER-" + suffix, "Controller Purchase Book", null,
                new BigDecimal("80.00"), 4, 0));
        productId = product.getId();

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":%d,"items":[
                                  {"productId":%d,"quantity":5,"unitCost":50.25}
                                ]}
                                """.formatted(supplierId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supplierId").value(supplierId))
                .andExpect(jsonPath("$.data.status").value("ORDERED"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.items[0].unitCost").value(50.25));

        PurchaseOrder created = purchaseOrderRepository
                .findAllBySupplierIdOrderByOrderDateDesc(supplierId).getFirst();
        purchaseOrderId = created.getId();

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(purchaseOrderId))
                .andExpect(jsonPath("$.data.status").value("ORDERED"));

        assertEquals(4, productRepository.findById(productId).orElseThrow().getQuantity());

        mockMvc.perform(post("/api/purchase-orders/{id}/receive", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.receivedDate").isNotEmpty());

        assertEquals(9, productRepository.findById(productId).orElseThrow().getQuantity());

        mockMvc.perform(post("/api/purchase-orders/{id}/receive", purchaseOrderId))
                .andExpect(status().isConflict());

        assertEquals(9, productRepository.findById(productId).orElseThrow().getQuantity());
        assertEquals(PurchaseOrderStatus.RECEIVED,
                purchaseOrderRepository.findById(purchaseOrderId).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "purchase-controller-staff", roles = "STAFF")
    void staffCannotCreateListViewOrReceivePurchaseOrders() throws Exception {
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":1,"items":[
                                  {"productId":1,"quantity":1,"unitCost":10.00}
                                ]}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/purchase-orders/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/purchase-orders/1/receive"))
                .andExpect(status().isForbidden());
    }
}
