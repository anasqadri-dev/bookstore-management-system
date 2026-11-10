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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class SchoolOrderStatusServiceIntegrationTest {

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
    private EntityManager entityManager;

    private Integer schoolOrderId;
    private Integer schoolId;
    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();
        if (schoolOrderId != null) {
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
    void updatesOrderStatusToEachSupportedSdsValue() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Status School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "status-admin-" + suffix, "test-hash",
                "Status Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "STATUS-" + suffix, "Status Test Book", null,
                new BigDecimal("10.00"), 10, 1));
        productId = product.getId();

        SchoolOrderResponse order = schoolOrderService.create(
                new SchoolOrderRequest(schoolId,
                        List.of(new SchoolOrderItemRequest(productId, 1))),
                user.getUsername());
        schoolOrderId = order.id();

        assertEquals(SchoolOrderStatus.PENDING, order.status());
        assertEquals(SchoolOrderStatus.READY,
                schoolOrderService.updateStatus(schoolOrderId, SchoolOrderStatus.READY).status());
        assertEquals(SchoolOrderStatus.DELIVERED,
                schoolOrderService.updateStatus(schoolOrderId, SchoolOrderStatus.DELIVERED).status());
        assertEquals(SchoolOrderStatus.PENDING,
                schoolOrderService.updateStatus(schoolOrderId, SchoolOrderStatus.PENDING).status());

        entityManager.clear();
        assertEquals(SchoolOrderStatus.PENDING,
                schoolOrderRepository.findById(schoolOrderId).orElseThrow().getStatus());
    }

    @Test
    void rejectsNullStatusAndUnknownOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> schoolOrderService.updateStatus(1, null));
        assertThrows(com.power_puff.bms.common.exception.ResourceNotFoundException.class,
                () -> schoolOrderService.updateStatus(999999, SchoolOrderStatus.READY));
    }
}
