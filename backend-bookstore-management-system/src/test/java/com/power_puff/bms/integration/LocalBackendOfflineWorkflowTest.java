package com.power_puff.bms.integration;

import com.power_puff.bms.backup.dto.DatabaseBackupResult;
import com.power_puff.bms.backup.service.DatabaseBackupService;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.report.dto.InventoryReportResponse;
import com.power_puff.bms.report.dto.SalesReportResponse;
import com.power_puff.bms.report.service.ReportService;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.sale.service.SaleService;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the backend business flow uses only its local SQLite datasource.
 * This is backend integration evidence; it does not replace a disconnected
 * Windows/Electron end-to-end manual test.
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalBackendOfflineWorkflowTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private DatabaseBackupService backupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path temporaryDirectory;

    private Long userId;
    private Integer productId;
    private Integer saleId;

    @AfterEach
    void cleanUp() {
        if (saleId != null) {
            saleRepository.deleteById(saleId);
        }
        if (productId != null) {
            productRepository.deleteById(productId);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
    }

    @Test
    void localLoginDataSaleReportsAndBackupUseTheLocalSqliteFile() throws Exception {
        // Confirm the active datasource is a local file, not a remote database.
        String activeDatabasePath;
        try (var connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet databases = statement.executeQuery("PRAGMA database_list")) {
            assertTrue(databases.next());
            assertEquals("main", databases.getString("name"));
            activeDatabasePath = databases.getString("file");
        }

        assertNotNull(activeDatabasePath);
        assertTrue(!activeDatabasePath.isBlank());
        Path activeDatabase = Path.of(activeDatabasePath).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(activeDatabase),
                "The backend database must be a local SQLite file.");

        String suffix = UUID.randomUUID().toString();
        String username = "offline-" + suffix;
        User cashier = userRepository.saveAndFlush(new User(
                username, "already-encoded", "Offline Workflow Cashier", UserRole.ADMIN));
        userId = cashier.getId();

        Product book = productRepository.saveAndFlush(new Product(
                "OFFLINE-" + suffix,
                "Offline Workflow Book",
                "Local Author",
                new BigDecimal("125.00"),
                10,
                2));
        productId = book.getId();

        // Checkout, stock deduction, receipt generation, and audit write are local.
        SaleReceiptResponse receipt = saleService.create(
                new SaleRequest("Offline Customer", List.of(
                        new SaleItemRequest(book.getId(), 2))),
                username);
        saleId = receipt.id();

        assertEquals(0, new BigDecimal("250.00").compareTo(receipt.totalAmount()));
        assertEquals(1, receipt.items().size());
        assertEquals(8, productRepository.findById(book.getId()).orElseThrow().getQuantity());

        ZoneId shopTimeZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(shopTimeZone);
        SalesReportResponse salesReport = reportService.getSalesReport(today, today);
        assertTrue(salesReport.sales().stream()
                .anyMatch(row -> row.saleId().equals(receipt.id())));

        InventoryReportResponse inventoryReport = reportService.getInventoryReport();
        var productRow = inventoryReport.products().stream()
                .filter(row -> row.productId().equals(book.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(8, productRow.quantity());
        assertEquals(0, new BigDecimal("1000.00")
                .compareTo(productRow.stockValue()));

        // Backup the same local database without any cloud or external storage service.
        Path backupPath = temporaryDirectory.resolve("offline-workflow.db");
        DatabaseBackupResult backup = backupService.createBackup(backupPath.toString());
        assertTrue(Files.isRegularFile(backupPath));
        assertTrue(backup.sizeBytes() > 0);

        try (var backupConnection =
                     DriverManager.getConnection("jdbc:sqlite:" + backupPath);
             Statement statement = backupConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM sales WHERE id = " + receipt.id())) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
