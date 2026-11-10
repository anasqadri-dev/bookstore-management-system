package com.power_puff.bms.school.repository;

import com.power_puff.bms.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Integer> {

    List<School> findAllByOrderByNameAsc();
}
