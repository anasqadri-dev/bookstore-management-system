package com.power_puff.bms.schoolorder.service;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.dto.SchoolOrderItemRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SchoolOrderServiceIntegrationTest {

    @Autowired
    private SchoolOrderService schoolOrderService;

    @Autowired
    private SchoolOrderRepository schoolOrderRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Integer schoolOrderId;
    private Integer schoolId;
    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();
        if (schoolOrderId != null) {
            jdbcTemplate.update("DELETE FROM school_order_items WHERE order_id = ?", schoolOrderId);
            schoolOrderRepository.deleteById(schoolOrderId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void createsPendingOrderWithAggregatedItemsAndPriceSnapshots() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Create Order School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "create-school-order-" + suffix, "test-hash",
                "Order Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "ORDER-CREATE-" + suffix, "Order Creation Book", null,
                new BigDecimal("12.50"), 50, 2));
        productId = product.getId();

        SchoolOrderRequest request = new SchoolOrderRequest(
                schoolId,
                List.of(
                        new SchoolOrderItemRequest(productId, 2),
                        new SchoolOrderItemRequest(productId, 3)));

        SchoolOrderResponse response = schoolOrderService.create(request, user.getUsername());
        schoolOrderId = response.id();

        assertEquals(schoolId, response.schoolId());
        assertEquals("Create Order School " + suffix, response.schoolName());
        assertEquals(SchoolOrderStatus.PENDING, response.status());
        assertEquals("Order Admin", response.createdBy());
        assertEquals(1, response.items().size());
        assertEquals(5, response.items().getFirst().quantity());
        assertEquals(0, new BigDecimal("12.50").compareTo(
                response.items().getFirst().unitPrice()));

        Product repriced = productRepository.findById(productId).orElseThrow();
        repriced.setPrice(new BigDecimal("14.00"));
        productRepository.saveAndFlush(repriced);
        entityManager.clear();

        SchoolOrderResponse saved = schoolOrderService.getById(schoolOrderId);
        assertEquals(0, new BigDecimal("12.50").compareTo(
                saved.items().getFirst().unitPrice()));
        assertEquals(50, productRepository.findById(productId).orElseThrow().getQuantity());
    }

    @Test
    void rejectsSchoolOrderWhenRequestedQuantityExceedsAvailableStock() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Insufficient Stock School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "insufficient-stock-" + suffix, "test-hash",
                "Order Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "INSUFFICIENT-" + suffix, "Limited School Book", null,
                new BigDecimal("30.00"), 2, 1));
        productId = product.getId();

        long ordersBefore = schoolOrderRepository.count();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> schoolOrderService.create(
                        new SchoolOrderRequest(schoolId, List.of(
                                new SchoolOrderItemRequest(productId, 3))),
                        user.getUsername()));

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertEquals(2, productRepository.findById(productId).orElseThrow().getQuantity());
        assertEquals(ordersBefore, schoolOrderRepository.count());
        assertTrue(schoolOrderRepository.findAllBySchoolIdOrderByOrderDateDesc(schoolId).isEmpty());
    }

    @Test
    void checksAggregatedQuantityAcrossRepeatedProductRows() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Repeated Quantity School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "repeated-quantity-" + suffix, "test-hash",
                "Order Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "REPEATED-" + suffix, "Repeated Quantity Book", null,
                new BigDecimal("10.00"), 3, 1));
        productId = product.getId();

        long ordersBefore = schoolOrderRepository.count();
        assertThrows(IllegalArgumentException.class,
                () -> schoolOrderService.create(
                        new SchoolOrderRequest(schoolId, List.of(
                                new SchoolOrderItemRequest(productId, 2),
                                new SchoolOrderItemRequest(productId, 2))),
                        user.getUsername()));

        assertEquals(3, productRepository.findById(productId).orElseThrow().getQuantity());
        assertEquals(ordersBefore, schoolOrderRepository.count());
    }

    @Test
    void allowsSchoolOrderWhenRequestedQuantityExactlyMatchesAvailableStock() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Exact Stock School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "exact-school-stock-" + suffix, "test-hash",
                "Order Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "EXACT-STOCK-" + suffix, "Exact Stock School Book", null,
                new BigDecimal("25.00"), 4, 1));
        productId = product.getId();

        SchoolOrderResponse response = schoolOrderService.create(
                new SchoolOrderRequest(schoolId, List.of(
                        new SchoolOrderItemRequest(productId, 4))),
                user.getUsername());
        schoolOrderId = response.id();

        assertEquals(4, response.items().getFirst().quantity());
        assertEquals(4, productRepository.findById(productId).orElseThrow().getQuantity());
    }

    @Test
    void rejectsSchoolOrderWithoutItems() {
        assertThrows(IllegalArgumentException.class,
                () -> schoolOrderService.create(new SchoolOrderRequest(1, List.of()), "admin"));
    }

    @Test
    void rejectsUnknownSchool() {
        assertThrows(com.power_puff.bms.common.exception.ResourceNotFoundException.class,
                () -> schoolOrderService.create(
                        new SchoolOrderRequest(999999, List.of(
                                new SchoolOrderItemRequest(1, 1))), "admin"));
    }

    @Test
    void rejectsUnknownProduct() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Missing Product School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "missing-product-" + suffix, "test-hash",
                "Order Admin", UserRole.ADMIN));
        userId = user.getId();

        assertThrows(com.power_puff.bms.common.exception.ResourceNotFoundException.class,
                () -> schoolOrderService.create(
                        new SchoolOrderRequest(schoolId, List.of(
                                new SchoolOrderItemRequest(999999, 1))),
                        user.getUsername()));
        assertTrue(schoolOrderRepository.findAllBySchoolIdOrderByOrderDateDesc(schoolId).isEmpty());
    }
}
