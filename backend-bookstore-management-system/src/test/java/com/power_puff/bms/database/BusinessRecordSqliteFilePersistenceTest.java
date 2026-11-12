package com.power_puff.bms.database;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.entity.SaleItem;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/bms-business-record-persistence-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class BusinessRecordSqliteFilePersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SchoolOrderRepository schoolOrderRepository;

    @Test
    void committedSalesAndSchoolOrdersCanBeReadThroughANewSqliteConnection()
            throws Exception {
        String suffix = UUID.randomUUID().toString();

        User user = userRepository.saveAndFlush(new User(
                "file-persistence-" + suffix,
                "already-encoded",
                "Persistence Test Admin",
                UserRole.ADMIN));

        Product product = productRepository.saveAndFlush(new Product(
                "FILE-RECORD-" + suffix,
                "File Persistence Book",
                null,
                new BigDecimal("12.50"),
                20,
                2));

        School school = schoolRepository.saveAndFlush(new School(
                "Persistence School " + suffix,
                null,
                null,
                null));

        Sale sale = new Sale(
                Instant.parse("2026-11-12T05:00:00Z"),
                "Persistence Customer",
                new BigDecimal("25.00"),
                user);
        sale.addItem(new SaleItem(
                product, 2, new BigDecimal("12.50"), new BigDecimal("25.00")));
        Sale savedSale = saleRepository.saveAndFlush(sale);

        SchoolOrder schoolOrder = new SchoolOrder(school, user);
        schoolOrder.addItem(new SchoolOrderItem(
                product, 3, new BigDecimal("12.50")));
        SchoolOrder savedSchoolOrder = schoolOrderRepository.saveAndFlush(schoolOrder);

        try (var connection = DriverManager.getConnection(
                "jdbc:sqlite:./target/bms-business-record-persistence-test.db")) {
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM sales WHERE id = ?", savedSale.getId()));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM sale_items WHERE sale_id = ?", savedSale.getId()));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM school_orders WHERE id = ?", savedSchoolOrder.getId()));
            assertEquals(1, count(connection,
                    "SELECT COUNT(*) FROM school_order_items WHERE order_id = ?",
                    savedSchoolOrder.getId()));
        }
    }

    private int count(java.sql.Connection connection, String sql, Integer id)
            throws Exception {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
