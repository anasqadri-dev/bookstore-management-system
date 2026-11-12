package com.power_puff.bms.school.controller;

import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolRepository schoolRepository;

    private Integer createdSchoolId;

    @AfterEach
    void cleanUp() {
        if (createdSchoolId != null) {
            schoolRepository.deleteById(createdSchoolId);
        }
    }

    @Test
    @WithMockUser(username = "school-admin", roles = "ADMIN")
    void adminCanRegisterAndListSchools() throws Exception {
        String schoolName = "API School Admin Test";

        mockMvc.perform(post("/api/schools").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","contactPerson":"Ayesha Khan",
                                 "phone":"03001234567","address":"Main Road"}
                                """.formatted(schoolName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(schoolName))
                .andExpect(jsonPath("$.data.contactPerson").value("Ayesha Khan"));

        School saved = schoolRepository.findAllByOrderByNameAsc().stream()
                .filter(school -> school.getName().equals(schoolName))
                .findFirst()
                .orElseThrow();
        createdSchoolId = saved.getId();

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "school-staff", roles = "STAFF")
    void staffCannotRegisterOrListSchools() throws Exception {
        mockMvc.perform(post("/api/schools").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Staff Must Not Register"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/schools"))
                .andExpect(status().isForbidden());

        assertEquals(0, schoolRepository.findAllByOrderByNameAsc().stream()
                .filter(school -> school.getName().equals("Staff Must Not Register"))
                .count());
    }

    @Test
    @WithMockUser(username = "school-admin", roles = "ADMIN")
    void rejectsSchoolRegistrationWithoutAName() throws Exception {
        mockMvc.perform(post("/api/schools").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","contactPerson":"Ayesha"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists());
    }
}
