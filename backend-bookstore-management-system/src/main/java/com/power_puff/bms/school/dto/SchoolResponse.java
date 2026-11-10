package com.power_puff.bms.school.dto;

public record SchoolResponse(
        Integer id,
        String name,
        String contactPerson,
        String phone,
        String address
) {
}
