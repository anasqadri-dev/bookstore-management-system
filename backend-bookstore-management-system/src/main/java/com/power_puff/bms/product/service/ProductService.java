package com.power_puff.bms.product.service;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.dto.ProductRequest;
import com.power_puff.bms.product.dto.ProductResponse;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.mapper.ProductMapper;
import com.power_puff.bms.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductUsageChecker productUsageChecker;

    public ProductService(ProductRepository productRepository,
                          ProductUsageChecker productUsageChecker) {
        this.productRepository = productRepository;
        this.productUsageChecker = productUsageChecker;
    }

    public ProductResponse create(ProductRequest request) {
        String normalizedSku = normalizeSku(request.sku());
        ensureSkuAvailable(normalizedSku, null);

        Product product = ProductMapper.toEntity(request);
        product.setSku(normalizedSku);
        try {
            return ProductMapper.toResponse(productRepository.save(product));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("A product with this SKU already exists.");
        }
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        String normalizedSku = normalizeSku(request.sku());
        ensureSkuAvailable(normalizedSku, id);

        ProductMapper.updateEntity(product, request);
        product.setSku(normalizedSku);
        try {
            return ProductMapper.toResponse(productRepository.save(product));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("A product with this SKU already exists.");
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String search) {
        List<Product> products = (search == null || search.isBlank())
                ? productRepository.findAllByOrderByTitleAsc()
                : productRepository.findByTitleContainingIgnoreCaseOrSkuContainingIgnoreCase(
                        search.trim(), search.trim());
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findAllLowStockProducts()
                .stream().map(ProductMapper::toResponse).toList();
    }

    public void delete(Long id) {
        Product product = getEntity(id);
        if (productUsageChecker.isReferenced(id)) {
            throw new DuplicateResourceException(
                    "This product cannot be deleted because it is used in an existing sale or order.");
        }
        productRepository.delete(product);
    }

    private Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id " + id + " was not found."));
    }

    private void ensureSkuAvailable(String sku, Long currentProductId) {
        productRepository.findBySkuIgnoreCase(sku).ifPresent(existing -> {
            if (currentProductId == null || !existing.getId().equals(currentProductId)) {
                throw new DuplicateResourceException("A product with this SKU already exists.");
            }
        });
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
