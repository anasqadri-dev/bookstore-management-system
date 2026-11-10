package com.power_puff.bms.product;

import com.power_puff.bms.common.exception.GlobalExceptionHandler;
import com.power_puff.bms.product.controller.ProductController;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.product.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sale_items " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER NOT NULL)");
        jdbcTemplate.update("DELETE FROM sale_items");
        jdbcTemplate.update("DELETE FROM products");
        entityManager.clear();

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM sale_items");
        entityManager.clear();
        if (validator != null) {
            validator.close();
        }
    }

    @Test
    void createsAndRetrievesProductThroughApi() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("BK-300", "Integration Book", 7, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("BK-300"))
                .andExpect(jsonPath("$.data.quantity").value(7));

        Product saved = productRepository.findBySkuIgnoreCase("BK-300").orElseThrow();

        mockMvc.perform(get("/api/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Integration Book"));
    }

    @Test
    void rejectsDuplicateSkuWithConflictResponse() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductJson("BK-301", "First", 5, 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductJson("bk-301", "Duplicate", 4, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsInvalidProductRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":" ","title":"","author":null,"price":0,"quantity":-1,"minStock":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.sku").exists());
    }

    @Test
    void updatesExistingProduct() throws Exception {
        Product saved = productRepository.saveAndFlush(new Product(
                "BK-302", "Old title", null, new BigDecimal("50.00"), 5, 2));

        mockMvc.perform(put("/api/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("BK-302", "New title", 9, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New title"))
                .andExpect(jsonPath("$.data.quantity").value(9));
    }

    @Test
    void searchesByTitleOrSkuAndReturnsEmptyForNoMatch() throws Exception {
        productRepository.saveAndFlush(new Product(
                "SKU-RED", "Red Notebook", null, new BigDecimal("25.00"), 10, 2));
        productRepository.saveAndFlush(new Product(
                "SKU-BLUE", "Blue Notebook", null, new BigDecimal("30.00"), 8, 2));

        mockMvc.perform(get("/api/products").param("search", "Red"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-RED"));

        mockMvc.perform(get("/api/products").param("search", "MISSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void lowStockIncludesQuantityEqualToMinimumButExcludesAboveMinimum() throws Exception {
        productRepository.saveAndFlush(new Product(
                "LOW-1", "At Threshold", null, new BigDecimal("10.00"), 3, 3));
        productRepository.saveAndFlush(new Product(
                "LOW-2", "Above Threshold", null, new BigDecimal("10.00"), 4, 3));
        productRepository.saveAndFlush(new Product(
                "LOW-3", "Below Threshold", null, new BigDecimal("10.00"), 2, 3));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void blocksDeletionWhenProductIsUsedInSale() throws Exception {
        Product saved = productRepository.saveAndFlush(new Product(
                "USED-1", "Previously Sold", null, new BigDecimal("10.00"), 4, 1));
        jdbcTemplate.update("INSERT INTO sale_items(product_id) VALUES (?)", saved.getId());

        mockMvc.perform(delete("/api/products/{id}", saved.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        assertTrue(productRepository.existsById(saved.getId()));
    }

    @Test
    void deletesUnusedProduct() throws Exception {
        Product saved = productRepository.saveAndFlush(new Product(
                "UNUSED-1", "Unused Product", null, new BigDecimal("10.00"), 4, 1));

        mockMvc.perform(delete("/api/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(productRepository.existsById(saved.getId()));
    }

    @Test
    void inventorySearchRespondsWithinThreeSecondRequirement() throws Exception {
        List<Product> products = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            products.add(new Product(
                    "PERF-" + index, "Performance Test Book " + index, null,
                    new BigDecimal("50.00"), 10, 2));
        }
        productRepository.saveAllAndFlush(products);

        mockMvc.perform(get("/api/products").param("search", "Performance"))
                .andExpect(status().isOk());

        long startedAt = System.nanoTime();
        mockMvc.perform(get("/api/products").param("search", "Performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(25)));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertTrue(elapsedMillis <= 3_000,
                "Inventory search took " + elapsedMillis + " ms, exceeding NFR-01.");
    }

    @Test
    void repositoryPersistsUniqueSkuAndFindsLowStockRows() {
        Product product = productRepository.saveAndFlush(new Product(
                "persist-1", "Persisted Book", null,
                new BigDecimal("100.00"), 2, 2));

        assertTrue(productRepository.existsBySkuIgnoreCase("PERSIST-1"));
        assertEquals(product.getId(),
                productRepository.findBySkuIgnoreCase("persist-1").orElseThrow().getId());
        assertEquals(1, productRepository.findAllLowStockProducts().size());
    }

    private String validProductJson(String sku, String title, int quantity, int minStock) {
        return """
                {"sku":"%s","title":"%s","author":"Writer","price":125.00,"quantity":%d,"minStock":%d}
                """.formatted(sku, title, quantity, minStock);
    }
}
