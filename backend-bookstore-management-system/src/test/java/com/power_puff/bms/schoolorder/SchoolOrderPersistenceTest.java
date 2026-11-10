package com.power_puff.bms.schoolorder;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class SchoolOrderPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Integer schoolOrderId;
    private Integer schoolId;
    private Integer productId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();
        if (schoolOrderId != null) {
            SchoolOrder schoolOrder = entityManager.find(SchoolOrder.class, schoolOrderId);
            if (schoolOrder != null) {
                entityManager.remove(schoolOrder);
                entityManager.flush();
            }
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
    void persistsSchoolOrderAndItemsWithHistoricalUnitPrice() {
        String suffix = UUID.randomUUID().toString();

        School school = schoolRepository.saveAndFlush(new School(
                "Order School " + suffix, "Contact Person", null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "school-order-" + suffix, "test-hash",
                "School Order Admin", UserRole.ADMIN));
        userId = user.getId();

        Product product = productRepository.saveAndFlush(new Product(
                "SCHOOL-ORDER-" + suffix, "School Order Book", null,
                new BigDecimal("125.00"), 20, 2));
        productId = product.getId();

        SchoolOrder order = new SchoolOrder(
                school, LocalDate.of(2026, 11, 11),
                SchoolOrderStatus.PENDING, user);
        order.addItem(new SchoolOrderItem(
                product, 4, new BigDecimal("125.00")));

        entityManager.persist(order);
        entityManager.flush();
        schoolOrderId = order.getId();

        assertNotNull(schoolOrderId);
        assertEquals(SchoolOrderStatus.PENDING, order.getStatus());
        assertEquals(LocalDate.of(2026, 11, 11), order.getOrderDate());
        assertEquals(1, order.getItems().size());

        product.setPrice(new BigDecimal("150.00"));
        productRepository.saveAndFlush(product);
        entityManager.clear();

        SchoolOrder savedOrder = entityManager.find(SchoolOrder.class, schoolOrderId);
        SchoolOrderItem savedItem = savedOrder.getItems().getFirst();

        assertEquals("Order School " + suffix, savedOrder.getSchool().getName());
        assertEquals("School Order Admin", savedOrder.getUser().getFullName());
        assertEquals(4, savedItem.getQuantity());
        assertEquals(0, new BigDecimal("125.00").compareTo(savedItem.getUnitPrice()));
        assertEquals(0, new BigDecimal("150.00").compareTo(
                savedItem.getProduct().getPrice()));
    }

    @Test
    void defaultsNewOrderStatusToPending() {
        String suffix = UUID.randomUUID().toString();

        School school = schoolRepository.saveAndFlush(new School(
                "Default Status School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "school-order-default-" + suffix, "test-hash",
                "Default Status Admin", UserRole.ADMIN));
        userId = user.getId();

        SchoolOrder order = new SchoolOrder(school, user);
        entityManager.persist(order);
        entityManager.flush();
        schoolOrderId = order.getId();

        assertEquals(SchoolOrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getOrderDate());
    }
}
