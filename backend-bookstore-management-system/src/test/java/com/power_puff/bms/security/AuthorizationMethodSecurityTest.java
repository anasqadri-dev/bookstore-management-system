package com.power_puff.bms.security;

import com.power_puff.bms.product.controller.ProductController;
import com.power_puff.bms.product.dto.ProductRequest;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.user.controller.UserController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthorizationMethodSecurityTest {

    @Autowired
    private ProductController productController;

    @Autowired
    private UserController userController;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanProducts() {
        jdbcTemplate.update("DELETE FROM products");
    }

    @Test
    @WithMockUser(username = "staff1", roles = "STAFF")
    void staffCanReadProducts() {
        assertTrue(productController.search(null).success());
    }

    @Test
    @WithMockUser(username = "staff1", roles = "STAFF")
    void staffCannotCreateProducts() {
        ProductRequest request = new ProductRequest(
                "AUTH-1", "Unauthorized Book", null, new BigDecimal("10.00"), 1, 0);

        assertThrows(AccessDeniedException.class, () -> productController.create(request));
    }

    @Test
    @WithMockUser(username = "staff1", roles = "STAFF")
    void staffCannotManageUserAccounts() {
        assertThrows(AccessDeniedException.class, () -> userController.listUsers());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateProduct() {
        ProductRequest request = new ProductRequest(
                "AUTH-2", "Admin Book", null, new BigDecimal("10.00"), 2, 0);

        var response = productController.create(request);

        assertTrue(response.getBody().success());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku = ?", Integer.class, "AUTH-2"));
    }
}
