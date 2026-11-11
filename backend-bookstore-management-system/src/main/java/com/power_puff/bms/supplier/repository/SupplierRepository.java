package com.power_puff.bms.supplier.repository;

import com.power_puff.bms.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    List<Supplier> findAllByOrderByNameAsc();
}
