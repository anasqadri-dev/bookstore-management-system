package com.power_puff.bms.schoolorder.repository;

import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolOrderItemRepository extends JpaRepository<SchoolOrderItem, Integer> {

    List<SchoolOrderItem> findAllBySchoolOrderIdOrderByIdAsc(Integer schoolOrderId);
}
