package com.power_puff.bms.schoolorder.service;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.dto.SchoolOrderItemRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.dto.SchoolQuotationResponse;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.schoolorder.service.SchoolOrderService;
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
class SchoolQuotationServiceIntegrationTest {

    @Autowired
    private SchoolQuotationService schoolQuotationService;

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
    private Integer firstProductId;
    private Integer secondProductId;
    private Long userId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();
        if (schoolOrderId != null) {
            schoolOrderRepository.deleteById(schoolOrderId);
        }
        if (firstProductId != null) {
            productRepository.deleteById(firstProductId);
        }
        if (secondProductId != null) {
            productRepository.deleteById(secondProductId);
        }
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void generatesHandCalculatedQuotationFromHistoricalUnitPrices() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Quotation School " + suffix, null, null, null));
        schoolId = school.getId();

        User user = userRepository.saveAndFlush(new User(
                "quotation-admin-" + suffix, "test-hash",
                "Quotation Admin", UserRole.ADMIN));
        userId = user.getId();

        Product firstProduct = productRepository.saveAndFlush(new Product(
                "QUOTATION-A-" + suffix, "First Quotation Book", null,
                new BigDecimal("100.00"), 20, 1));
        firstProductId = firstProduct.getId();
        Product secondProduct = productRepository.saveAndFlush(new Product(
                "QUOTATION-B-" + suffix, "Second Quotation Book", null,
                new BigDecimal("50.00"), 20, 1));
        secondProductId = secondProduct.getId();

        SchoolOrderResponse order = schoolOrderService.create(
                new SchoolOrderRequest(schoolId, List.of(
                        new SchoolOrderItemRequest(firstProductId, 2),
                        new SchoolOrderItemRequest(secondProductId, 1))),
                user.getUsername());
        schoolOrderId = order.id();

        firstProduct.setPrice(new BigDecimal("125.00"));
        secondProduct.setPrice(new BigDecimal("75.00"));
        productRepository.saveAllAndFlush(List.of(firstProduct, secondProduct));
        entityManager.clear();

        SchoolQuotationResponse quotation = schoolQuotationService.generate(schoolOrderId);

        assertEquals("Quotation School " + suffix, quotation.schoolName());
        assertEquals(2, quotation.items().size());
        assertEquals(0, new BigDecimal("200.00").compareTo(
                quotation.items().get(0).lineTotal()));
        assertEquals(0, new BigDecimal("50.00").compareTo(
                quotation.items().get(1).lineTotal()));
        assertEquals(0, new BigDecimal("250.00").compareTo(quotation.totalAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                quotation.items().get(0).unitPrice()));
        assertEquals(0, new BigDecimal("50.00").compareTo(
                quotation.items().get(1).unitPrice()));
    }

    @Test
    void rejectsQuotationForUnknownSchoolOrder() {
        assertThrows(com.power_puff.bms.common.exception.ResourceNotFoundException.class,
                () -> schoolQuotationService.generate(999999));
    }
}
