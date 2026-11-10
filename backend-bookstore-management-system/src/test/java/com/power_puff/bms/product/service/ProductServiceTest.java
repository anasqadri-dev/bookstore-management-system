package com.power_puff.bms.product.service;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.dto.ProductRequest;
import com.power_puff.bms.product.dto.ProductResponse;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductUsageChecker productUsageChecker;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProductService productService;

    private ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ProductRequest(
                " bk-200 ", "Quality Book", "A. Writer",
                new BigDecimal("250.00"), 8, 3);
    }

    @Test
    void createsProductAndNormalizesSku() {
        when(productRepository.findBySkuIgnoreCase("BK-200")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(20L);
            return product;
        });

        ProductResponse response = productService.create(validRequest);

        assertEquals(20L, response.id());
        assertEquals("BK-200", response.sku());
        assertFalse(response.lowStock());
    }

    @Test
    void rejectsDuplicateSkuWithoutSaving() {
        Product existing = new Product("BK-200", "Existing", null,
                new BigDecimal("100.00"), 1, 1);
        existing.setId(1L);
        when(productRepository.findBySkuIgnoreCase("BK-200")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> productService.create(validRequest));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingProduct() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(999L, validRequest));
    }

    @Test
    void blocksDeletionWhenProductIsReferenced() {
        Product existing = new Product("BK-200", "Existing", null,
                new BigDecimal("100.00"), 1, 0);
        existing.setId(20L);
        when(productRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(productUsageChecker.isReferenced(20L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.delete(20L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deletesUnusedProduct() {
        Product existing = new Product("BK-200", "Existing", null,
                new BigDecimal("100.00"), 1, 0);
        existing.setId(20L);
        when(productRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(productUsageChecker.isReferenced(20L)).thenReturn(false);

        productService.delete(20L);

        verify(productRepository).delete(existing);
        verify(activityLogService).recordCurrentUserAction("DELETE", "Deleted product id=20");
    }

    @Test
    void searchesByTitleOrSku() {
        Product product = new Product("BK-200", "Quality Book", null,
                new BigDecimal("250.00"), 8, 3);
        product.setId(20L);
        when(productRepository.findByTitleContainingIgnoreCaseOrSkuContainingIgnoreCase(
                "quality", "quality")).thenReturn(List.of(product));

        List<ProductResponse> response = productService.search(" quality ");

        assertEquals(1, response.size());
        assertEquals("Quality Book", response.getFirst().title());
    }

    @Test
    void flagsQuantityEqualToMinimumAsLowStock() {
        Product product = new Product("BK-201", "Low Stock Book", null,
                new BigDecimal("10.00"), 3, 3);
        product.setId(21L);
        when(productRepository.findAllLowStockProducts()).thenReturn(List.of(product));

        List<ProductResponse> lowStock = productService.getLowStockProducts();

        assertEquals(1, lowStock.size());
        assertTrue(lowStock.getFirst().lowStock());
    }
}
