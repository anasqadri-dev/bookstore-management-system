package com.power_puff.bms.activitylog;

import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.product.service.ProductService;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.service.SaleService;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityLogWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleService saleService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Integer> productIds = new ArrayList<>();
    private final List<Integer> saleIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (Integer saleId : saleIds) {
            jdbcTemplate.update("DELETE FROM sale_items WHERE sale_id = ?", saleId);
            jdbcTemplate.update("DELETE FROM sales WHERE id = ?", saleId);
        }
        for (Long userId : userIds) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE user_id = ?", userId);
        }
        for (Integer productId : productIds) {
            productRepository.deleteById(productId);
        }
        for (Long userId : userIds) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void persistsLoginActivityForSuccessfulAuthenticatedLogin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User user = createUser("login-" + suffix, "Login Activity User");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"LoginPass123"}
                                """.formatted(user.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(1, countActivity(user.getId(), "LOGIN"));
    }

    @Test
    void persistsSaleActivityForSuccessfullySavedSale() {
        String suffix = UUID.randomUUID().toString();
        User user = createUser("sale-" + suffix, "Sale Activity User");
        Product product = createProduct("LOG-SALE-" + suffix, "Sale Activity Book");

        var receipt = saleService.create(
                new SaleRequest("Activity Log Customer", List.of(
                        new SaleItemRequest(product.getId(), 2))),
                user.getUsername());
        saleIds.add(receipt.id());

        assertEquals(1, countActivity(user.getId(), "SALE"));
    }

    @Test
    void persistsDeleteActivityForAuthenticatedProductDeletion() {
        String suffix = UUID.randomUUID().toString();
        User user = createUser("delete-" + suffix, "Delete Activity User");
        Product product = createProduct("LOG-DELETE-" + suffix, "Delete Activity Book");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        productService.delete(product.getId());

        assertEquals(1, countActivity(user.getId(), "DELETE"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id = ?", Integer.class, product.getId()));
    }

    private User createUser(String username, String fullName) {
        User user = userRepository.saveAndFlush(new User(
                username,
                passwordEncoder.encode("LoginPass123"),
                fullName,
                UserRole.ADMIN));
        userIds.add(user.getId());
        return user;
    }

    private Product createProduct(String sku, String title) {
        Product product = productRepository.saveAndFlush(new Product(
                sku, title, null, new BigDecimal("12.50"), 5, 1));
        productIds.add(product.getId());
        return product;
    }

    private long countActivity(Long userId, String action) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE user_id = ? AND action = ?",
                Long.class,
                userId,
                action);
    }
}
