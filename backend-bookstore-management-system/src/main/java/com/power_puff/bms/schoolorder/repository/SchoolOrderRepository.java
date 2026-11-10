package com.power_puff.bms.schoolorder.repository;

import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolOrderRepository extends JpaRepository<SchoolOrder, Integer> {

    List<SchoolOrder> findAllByOrderByOrderDateDesc();

    List<SchoolOrder> findAllBySchoolIdOrderByOrderDateDesc(Integer schoolId);

    List<SchoolOrder> findAllByStatusOrderByOrderDateDesc(SchoolOrderStatus status);
}
