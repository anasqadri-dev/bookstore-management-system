package com.power_puff.bms.report;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiPerformanceTest {

    private static final long NFR_01_LIMIT_MILLIS = 3_000;
    private static final int FIXTURE_SIZE = 200;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private final List<Integer> productIds = new ArrayList<>();
    private final List<Integer> saleIds = new ArrayList<>();
    private LocalDate reportDate;

    @BeforeEach
    void createRepresentativeLocalDataset() {
        String suffix = UUID.randomUUID().toString();
        User admin = userRepository.saveAndFlush(new User(
                "report-performance-" + suffix,
                "already-encoded",
                "Report Performance Admin",
                UserRole.ADMIN));
        userId = admin.getId();

        List<Product> products = new ArrayList<>();
        for (int index = 0; index < FIXTURE_SIZE; index++) {
            products.add(new Product(
                    "PERF-" + suffix + "-" + index,
                    "Performance Product " + index,
                    null,
                    new BigDecimal("15.25"),
                    10 + (index % 20),
                    5));
        }
        List<Product> savedProducts = productRepository.saveAll(products);
        productRepository.flush();
        savedProducts.forEach(product -> productIds.add(product.getId()));

        ZoneId shopTimeZone = ZoneId.of("Asia/Karachi");
        reportDate = LocalDate.now(shopTimeZone);
        Instant startOfDay = reportDate.atStartOfDay(shopTimeZone).toInstant();

        List<Sale> sales = new ArrayList<>();
        for (int index = 0; index < FIXTURE_SIZE; index++) {
            sales.add(new Sale(
                    startOfDay.plusSeconds(index * 60L),
                    "Performance Customer " + index,
                    new BigDecimal("25.00"),
                    admin));
        }
        List<Sale> savedSales = saleRepository.saveAll(sales);
        saleRepository.flush();
        savedSales.forEach(sale -> saleIds.add(sale.getId()));
    }

    @AfterEach
    void cleanUp() {
        if (!saleIds.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "DELETE FROM sales WHERE id = ?",
                    saleIds,
                    saleIds.size(),
                    (statement, saleId) -> statement.setInt(1, saleId));
        }
        if (!productIds.isEmpty()) {
            productRepository.deleteAllById(productIds);
        }
        if (userId != null) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
            userRepository.deleteById(userId);
        }
    }

    @Test
    void salesReportApiRespondsWithinNfr01ThreeSecondLimitForTwoHundredSales()
            throws Exception {
        long startedAt = System.nanoTime();

        MvcResult result = mockMvc.perform(get("/api/reports/sales")
                        .with(SecurityMockMvcRequestPostProcessors.user("report-performance")
                                .roles("ADMIN"))
                        .param("startDate", reportDate.toString())
                        .param("endDate", reportDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sales.length()").value(FIXTURE_SIZE))
                .andReturn();

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        assertTrue(result.getResponse().getContentAsString().length() > 0);
        assertTrue(elapsedMillis <= NFR_01_LIMIT_MILLIS,
                "Sales report took " + elapsedMillis
                        + " ms; NFR-01 limit is " + NFR_01_LIMIT_MILLIS + " ms.");
    }

    @Test
    void inventoryReportApiRespondsWithinNfr01ThreeSecondLimitForTwoHundredProducts()
            throws Exception {
        long startedAt = System.nanoTime();

        MvcResult result = mockMvc.perform(get("/api/reports/inventory")
                        .with(SecurityMockMvcRequestPostProcessors.user("report-performance")
                                .roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.products.length()").value(FIXTURE_SIZE))
                .andReturn();

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        assertTrue(result.getResponse().getContentAsString().length() > 0);
        assertTrue(elapsedMillis <= NFR_01_LIMIT_MILLIS,
                "Inventory report took " + elapsedMillis
                        + " ms; NFR-01 limit is " + NFR_01_LIMIT_MILLIS + " ms.");
    }
}
