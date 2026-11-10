package com.power_puff.bms.school;

import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SchoolPersistenceTest {

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
    void persistsAllSchoolDataDictionaryFields() {
        String suffix = UUID.randomUUID().toString();
        School school = schoolRepository.saveAndFlush(new School(
                "Community School " + suffix,
                "Ayesha Khan",
                "03001234567",
                "Main Road"));

        schoolId = school.getId();

        School saved = schoolRepository.findById(schoolId).orElseThrow();
        assertNotNull(saved.getId());
        assertEquals(school.getName(), saved.getName());
        assertEquals("Ayesha Khan", saved.getContactPerson());
        assertEquals("03001234567", saved.getPhone());
        assertEquals("Main Road", saved.getAddress());
    }

    @Test
    void listsSchoolsInNameOrder() {
        String suffix = UUID.randomUUID().toString();
        School second = schoolRepository.saveAndFlush(new School(
                "Zeta School " + suffix, null, null, null));
        School first = schoolRepository.saveAndFlush(new School(
                "Alpha School " + suffix, null, null, null));
        schoolId = first.getId();

        var names = schoolRepository.findAllByOrderByNameAsc().stream()
                .map(School::getName)
                .filter(name -> name.endsWith(suffix))
                .toList();

        assertEquals(2, names.size());
        assertTrue(names.getFirst().startsWith("Alpha"));
        assertTrue(names.getLast().startsWith("Zeta"));
        schoolRepository.deleteById(second.getId());
    }

    @Test
    void rejectsBlankOrMissingSchoolName() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new School(null, null, null, null)).isEmpty());
            assertFalse(validator.validate(new School("   ", null, null, null)).isEmpty());
            assertTrue(validator.validate(new School("Community School", null, null, null)).isEmpty());
        }
    }
}
