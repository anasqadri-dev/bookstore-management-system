package com.power_puff.bms.sale.repository;

import com.power_puff.bms.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    List<Sale> findAllByOrderBySaleDateDesc();

    List<Sale> findAllBySaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateDesc(
            Instant startInclusive, Instant endExclusive);
}
