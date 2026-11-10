package com.power_puff.bms.product;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/bms-file-persistence-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class ProductSqliteFilePersistenceTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void committedProductCanBeReadThroughANewSqliteConnection() throws Exception {
        String sku = "FILE-PERSISTENCE-01";
        Product product = productRepository.saveAndFlush(new Product(
                sku, "SQLite Persistence Test", null,
                new BigDecimal("75.00"), 6, 2));

        try (var connection = DriverManager.getConnection(
                "jdbc:sqlite:./target/bms-file-persistence-test.db");
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM products WHERE sku = ?")) {
            statement.setString(1, sku);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }
    }
}
