package com.power_puff.bms.sale.repository;

import com.power_puff.bms.sale.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.math.BigDecimal;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    List<Sale> findAllByOrderBySaleDateDesc();

    @EntityGraph(attributePaths = "user")
    List<Sale> findTop5ByOrderBySaleDateDesc();

    @Query("""
            select coalesce(sum(sale.totalAmount), 0)
            from Sale sale
            where sale.saleDate >= :startInclusive
              and sale.saleDate < :endExclusive
            """)
    BigDecimal sumTotalAmountBetween(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);

    List<Sale> findAllBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(
            Instant startInclusive, Instant endExclusive);
}
