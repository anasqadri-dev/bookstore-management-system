package com.power_puff.bms.product.mapper;

import com.power_puff.bms.product.dto.ProductRequest;
import com.power_puff.bms.product.dto.ProductResponse;
import com.power_puff.bms.product.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toEntity(ProductRequest request) {
        return new Product(
                request.sku(),
                request.title(),
                request.author(),
                request.price(),
                request.quantity(),
                request.minStock()
        );
    }

    public static void updateEntity(Product product, ProductRequest request) {
        product.setSku(request.sku());
        product.setTitle(request.title());
        product.setAuthor(request.author());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setMinStock(request.minStock());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getTitle(),
                product.getAuthor(),
                product.getPrice(),
                product.getQuantity(),
                product.getMinStock(),
                product.isLowStock()
        );
    }
}
