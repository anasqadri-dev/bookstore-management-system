package com.power_puff.bms.product.repository;

import com.power_puff.bms.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsBySkuIgnoreCase(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findAllByOrderByTitleAsc();

    List<Product> findByTitleContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String title, String sku);

    /**
     * Performs a conditional stock decrement in the database. The quantity
     * predicate is evaluated in the same SQL statement as the update, so a
     * stale value previously read by another transaction cannot oversell.
     *
     * @return one if the product existed and sufficient stock was decremented
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Product product
               set product.quantity = product.quantity - :decrementBy
             where product.id = :productId
               and product.quantity >= :decrementBy
            """)
    int decrementStockIfAvailable(
            @Param("productId") Integer productId,
            @Param("decrementBy") Integer decrementBy);

    /**
     * Atomically increments stock only when the result remains within the
     * supported Integer range.
     *
     * @return one if the increment was applied
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Product product
               set product.quantity = product.quantity + :incrementBy
             where product.id = :productId
               and product.quantity <= :maximumBeforeIncrement
            """)
    int incrementStockIfWithinRange(
            @Param("productId") Integer productId,
            @Param("incrementBy") Integer incrementBy,
            @Param("maximumBeforeIncrement") Integer maximumBeforeIncrement);

    @Query("select count(product) from Product product " +
            "where product.quantity <= product.minStock")
    long countLowStockProducts();

    @Query("select product from Product product " +
            "where product.quantity <= product.minStock order by product.title asc")
    List<Product> findAllLowStockProducts();
}
