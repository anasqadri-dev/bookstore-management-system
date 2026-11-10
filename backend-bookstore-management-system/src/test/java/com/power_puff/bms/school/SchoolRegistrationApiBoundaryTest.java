package com.power_puff.bms.school;

import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolRegistrationApiBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolRepository schoolRepository;

    private Integer schoolId;

    @AfterEach
    void cleanUp() {
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }

    @Test
    @WithMockUser(username = "school-boundary-admin", roles = "ADMIN")
    void allowsOmittedOptionalSchoolFieldsAndStoresThemAsNull() throws Exception {
        String schoolName = "Optional Fields School";

        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(schoolName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(schoolName))
                .andExpect(jsonPath("$.data.contactPerson").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.phone").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.address").value(org.hamcrest.Matchers.nullValue()));

        School saved = schoolRepository.findAllByOrderByNameAsc().stream()
                .filter(school -> school.getName().equals(schoolName))
                .findFirst()
                .orElseThrow();
        schoolId = saved.getId();

        assertNull(saved.getContactPerson());
        assertNull(saved.getPhone());
        assertNull(saved.getAddress());
    }
}
