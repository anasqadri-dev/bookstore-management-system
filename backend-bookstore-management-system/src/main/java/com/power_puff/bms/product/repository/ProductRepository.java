package com.power_puff.bms.product.repository;

import com.power_puff.bms.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findAllByOrderByTitleAsc();

    List<Product> findByTitleContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String title, String sku);

    @Query("select product from Product product " +
            "where product.quantity <= product.minStock order by product.title asc")
    List<Product> findAllLowStockProducts();
}
