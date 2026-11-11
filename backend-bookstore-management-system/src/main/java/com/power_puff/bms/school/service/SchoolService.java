package com.power_puff.bms.school.service;

import com.power_puff.bms.school.dto.SchoolRequest;
import com.power_puff.bms.school.dto.SchoolResponse;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public SchoolResponse register(SchoolRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("School name is required.");
        }

        School school = new School(
                request.name().trim(),
                normalizeOptional(request.contactPerson()),
                normalizeOptional(request.phone()),
                normalizeOptional(request.address()));

        return toResponse(schoolRepository.saveAndFlush(school));
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> getAll() {
        return schoolRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SchoolResponse toResponse(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getContactPerson(),
                school.getPhone(),
                school.getAddress());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
