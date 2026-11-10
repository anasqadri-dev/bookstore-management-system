package com.power_puff.bms.schoolorder;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import com.power_puff.bms.schoolorder.repository.SchoolOrderItemRepository;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchoolOrderRepositoryTest {

    @Autowired
    private SchoolOrderRepository schoolOrderRepository;

    @Autowired
    private SchoolOrderItemRepository schoolOrderItemRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsOrdersBySchoolStatusAndOrderDateAndFindsTheirItems() {
        String suffix = UUID.randomUUID().toString();

        School school = schoolRepository.saveAndFlush(new School(
                "Repository School " + suffix, null, null, null));
        User user = userRepository.saveAndFlush(new User(
                "school-order-repo-" + suffix, "test-hash",
                "School Order Admin", UserRole.ADMIN));
        Product product = productRepository.saveAndFlush(new Product(
                "SCHOOL-REPO-" + suffix, "Repository Test Book", null,
                new BigDecimal("50.00"), 10, 1));

        SchoolOrder order = new SchoolOrder(
                school, LocalDate.of(2026, 11, 11),
                SchoolOrderStatus.READY, user);
        order.addItem(new SchoolOrderItem(product, 3, new BigDecimal("50.00")));
        SchoolOrder saved = schoolOrderRepository.saveAndFlush(order);

        assertEquals(1, schoolOrderRepository.findAllBySchoolIdOrderByOrderDateDesc(
                school.getId()).size());
        assertEquals(saved.getId(),
                schoolOrderRepository.findAllByStatusOrderByOrderDateDesc(
                        SchoolOrderStatus.READY).stream()
                        .filter(row -> row.getId().equals(saved.getId()))
                        .findFirst().orElseThrow().getId());
        assertEquals(saved.getId(),
                schoolOrderRepository.findAllByOrderByOrderDateDesc().stream()
                        .filter(row -> row.getId().equals(saved.getId()))
                        .findFirst().orElseThrow().getId());

        var items = schoolOrderItemRepository.findAllBySchoolOrderIdOrderByIdAsc(
                saved.getId());
        assertEquals(1, items.size());
        assertEquals(3, items.getFirst().getQuantity());
        assertEquals(0, new BigDecimal("50.00").compareTo(items.getFirst().getUnitPrice()));
    }
}
