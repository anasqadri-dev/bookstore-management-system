package com.power_puff.bms.sale.repository;

import com.power_puff.bms.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    List<SaleItem> findAllBySaleId(Integer saleId);
}
