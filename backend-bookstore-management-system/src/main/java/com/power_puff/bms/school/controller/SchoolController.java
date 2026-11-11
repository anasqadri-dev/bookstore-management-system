package com.power_puff.bms.school.controller;

import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.school.dto.SchoolRequest;
import com.power_puff.bms.school.dto.SchoolResponse;
import com.power_puff.bms.school.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@PreAuthorize("hasRole('ADMIN')")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SchoolResponse>> registerSchool(
            @Valid @RequestBody SchoolRequest request) {
        SchoolResponse school = schoolService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("School registered successfully.", school));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolResponse>>> getSchools() {
        return ResponseEntity.ok(ApiResponse.success(
                "Schools retrieved successfully.", schoolService.getAll()));
    }
}
