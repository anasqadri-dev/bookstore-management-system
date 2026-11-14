package com.power_puff.bms.database;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.dto.PurchaseOrderItemRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.service.PurchaseOrderService;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.service.SaleService;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.dto.SchoolOrderItemRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.service.SchoolOrderService;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/bms-currency-precision-${random.uuid}.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class CurrencyPrecisionSqliteRoundTripTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SchoolOrderService schoolOrderService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Integer> productIds = new ArrayList<>();
    private final List<Integer> saleIds = new ArrayList<>();
    private final List<Integer> schoolOrderIds = new ArrayList<>();
    private final List<Integer> purchaseOrderIds = new ArrayList<>();
    private Long userId;
    private Integer schoolId;
    private Integer supplierId;

    @AfterEach
    void cleanUp() {
        entityManager.clear();

        for (Integer saleId : saleIds) {
            jdbcTemplate.update("DELETE FROM sale_items WHERE sale_id = ?", saleId);
            jdbcTemplate.update("DELETE FROM sales WHERE id = ?", saleId);
        }
        for (Integer schoolOrderId : schoolOrderIds) {
            jdbcTemplate.update("DELETE FROM school_order_items WHERE order_id = ?", schoolOrderId);
            jdbcTemplate.update("DELETE FROM school_orders WHERE id = ?", schoolOrderId);
        }
        for (Integer purchaseOrderId : purchaseOrderIds) {
            jdbcTemplate.update(
                    "DELETE FROM purchase_order_items WHERE purchase_order_id = ?",
                    purchaseOrderId);
            jdbcTemplate.update("DELETE FROM purchase_orders WHERE id = ?", purchaseOrderId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
        }
        for (Integer productId : productIds) {
            productRepository.deleteById(productId);
        }
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
        if (supplierId != null) {
            supplierRepository.deleteById(supplierId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        entityManager.clear();
    }

    @Test
    @Transactional
    void preservesMoneyAcrossFileBackedSqliteReloadsAndJsonSerialization() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User admin = userRepository.saveAndFlush(new User(
                "currency-admin-" + suffix,
                "test-hash",
                "Currency Precision Admin",
                UserRole.ADMIN));
        userId = admin.getId();

        Product dimeBook = createProduct("CURRENCY-DIME-" + suffix,
                "Dime Book", new BigDecimal("0.10"));
        Product standardBook = createProduct("CURRENCY-STD-" + suffix,
                "Standard Book", new BigDecimal("19.99"));

        SaleReceiptResponse createdSale = saleService.create(
                new SaleRequest("Currency Customer", List.of(
                        new SaleItemRequest(dimeBook.getId(), 3),
                        new SaleItemRequest(standardBook.getId(), 2))),
                admin.getUsername());
        saleIds.add(createdSale.id());

        assertMoney("40.28", createdSale.totalAmount());
        assertMoney("0.30", itemPrice(createdSale, dimeBook.getId(), true));
        assertMoney("19.99", itemPrice(createdSale, standardBook.getId(), false));
        assertMoney("39.98", itemPrice(createdSale, standardBook.getId(), true));

        School school = schoolRepository.saveAndFlush(new School(
                "Currency School " + suffix, null, null, null));
        schoolId = school.getId();
        SchoolOrderResponse createdSchoolOrder = schoolOrderService.create(
                new SchoolOrderRequest(schoolId, List.of(
                        new SchoolOrderItemRequest(dimeBook.getId(), 3),
                        new SchoolOrderItemRequest(standardBook.getId(), 2))),
                admin.getUsername());
        schoolOrderIds.add(createdSchoolOrder.id());

        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Currency Supplier " + suffix, null, null));
        supplierId = supplier.getId();
        PurchaseOrderResponse createdPurchaseOrder = purchaseOrderService.create(
                new PurchaseOrderRequest(supplierId, List.of(
                        new PurchaseOrderItemRequest(
                                dimeBook.getId(), 3, new BigDecimal("0.20")),
                        new PurchaseOrderItemRequest(
                                standardBook.getId(), 2, new BigDecimal("19.99")))),
                admin.getUsername());
        purchaseOrderIds.add(createdPurchaseOrder.id());

        // Reprice the catalog after order creation. Historical transaction
        // prices must remain the values that were saved with each record.
        Product repricedDime = productRepository.findById(dimeBook.getId()).orElseThrow();
        repricedDime.setPrice(new BigDecimal("0.11"));
        productRepository.saveAndFlush(repricedDime);

        Product repricedStandard = productRepository.findById(standardBook.getId()).orElseThrow();
        repricedStandard.setPrice(new BigDecimal("20.00"));
        productRepository.saveAndFlush(repricedStandard);

        entityManager.flush();
        entityManager.clear();

        // Force a JDBC connection separate from the EntityManager's managed
        // state and verify the NUMERIC values survived the SQLite file write.
        assertJdbcMoney("SELECT price FROM products WHERE id = ?", "0.11", dimeBook.getId());
        assertJdbcMoney("SELECT total_amount FROM sales WHERE id = ?", "40.28",
                createdSale.id());
        assertJdbcMoney("""
                SELECT subtotal FROM sale_items
                WHERE sale_id = ? AND product_id = ?
                """, "0.30", createdSale.id(), dimeBook.getId());
        assertJdbcMoney("""
                SELECT unit_price FROM sale_items
                WHERE sale_id = ? AND product_id = ?
                """, "0.10", createdSale.id(), dimeBook.getId());
        assertJdbcMoney("""
                SELECT unit_price FROM school_order_items
                WHERE order_id = ? AND product_id = ?
                """, "0.10", createdSchoolOrder.id(), dimeBook.getId());
        assertJdbcMoney("""
                SELECT unit_cost FROM purchase_order_items
                WHERE purchase_order_id = ? AND product_id = ?
                """, "0.20", createdPurchaseOrder.id(), dimeBook.getId());

        SaleReceiptResponse reloadedSale = saleService.getReceipt(createdSale.id());
        assertMoney("40.28", reloadedSale.totalAmount());
        assertMoney("0.10", itemPrice(reloadedSale, dimeBook.getId(), false));
        assertMoney("19.99", itemPrice(reloadedSale, standardBook.getId(), false));

        SchoolOrderResponse reloadedSchoolOrder =
                schoolOrderService.getById(createdSchoolOrder.id());
        assertMoney("0.10", reloadedSchoolOrder.items().stream()
                .filter(item -> item.productId().equals(dimeBook.getId()))
                .findFirst().orElseThrow().unitPrice());
        assertMoney("19.99", reloadedSchoolOrder.items().stream()
                .filter(item -> item.productId().equals(standardBook.getId()))
                .findFirst().orElseThrow().unitPrice());

        PurchaseOrderResponse reloadedPurchaseOrder =
                purchaseOrderService.getById(createdPurchaseOrder.id());
        assertMoney("0.20", reloadedPurchaseOrder.items().stream()
                .filter(item -> item.productId().equals(dimeBook.getId()))
                .findFirst().orElseThrow().unitCost());
        assertMoney("19.99", reloadedPurchaseOrder.items().stream()
                .filter(item -> item.productId().equals(standardBook.getId()))
                .findFirst().orElseThrow().unitCost());

        assertMoney("0.11",
                productRepository.findById(dimeBook.getId()).orElseThrow().getPrice());
        assertMoney("20.00",
                productRepository.findById(standardBook.getId()).orElseThrow().getPrice());

        JsonNode receiptJson = objectMapper.readTree(
                objectMapper.writeValueAsBytes(reloadedSale));
        assertMoney("40.28", receiptJson.get("totalAmount").decimalValue());
        assertMoney("0.10", receiptJson.get("items").get(0).get("unitPrice").decimalValue());
        assertMoney("0.30", receiptJson.get("items").get(0).get("subtotal").decimalValue());
        assertNotNull(receiptJson.get("saleDate"));
    }

    private Product createProduct(String sku, String title, BigDecimal price) {
        Product saved = productRepository.saveAndFlush(new Product(
                sku, title, null, price, 50, 2));
        productIds.add(saved.getId());
        return saved;
    }

    private BigDecimal itemPrice(SaleReceiptResponse receipt,
                                 Integer productId,
                                 boolean subtotal) {
        var item = receipt.items().stream()
                .filter(candidate -> candidate.productId().equals(productId))
                .findFirst()
                .orElseThrow();
        return subtotal ? item.subtotal() : item.unitPrice();
    }

    private void assertJdbcMoney(String sql, String expected, Object... parameters) {
        BigDecimal actual = jdbcTemplate.queryForObject(
                sql,
                (resultSet, rowNumber) -> resultSet.getBigDecimal(1),
                parameters);
        assertMoney(expected, actual);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Expected exact persisted currency amount " + expected + " but got " + actual);
    }
}
