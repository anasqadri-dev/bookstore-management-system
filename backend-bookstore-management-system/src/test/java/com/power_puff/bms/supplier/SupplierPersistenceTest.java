package com.power_puff.bms.supplier;

import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
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
class SupplierPersistenceTest {

    @Autowired
    private SupplierRepository supplierRepository;

    private Integer supplierId;

    @AfterEach
    void cleanUp() {
        if (supplierId != null) {
            supplierRepository.deleteById(supplierId);
        }
    }

    @Test
    void persistsTheSupplierDataDictionaryFields() {
        String suffix = UUID.randomUUID().toString();
        Supplier supplier = supplierRepository.saveAndFlush(new Supplier(
                "Book Distributor " + suffix,
                "03001234567",
                "Main Market Road"));

        supplierId = supplier.getId();

        Supplier saved = supplierRepository.findById(supplierId).orElseThrow();
        assertNotNull(saved.getId());
        assertEquals("Book Distributor " + suffix, saved.getName());
        assertEquals("03001234567", saved.getPhone());
        assertEquals("Main Market Road", saved.getAddress());
    }

    @Test
    void listsSuppliersInNameOrder() {
        String suffix = UUID.randomUUID().toString();
        Supplier second = supplierRepository.saveAndFlush(
                new Supplier("Zeta Supplier " + suffix, null, null));
        Supplier first = supplierRepository.saveAndFlush(
                new Supplier("Alpha Supplier " + suffix, null, null));
        supplierId = first.getId();

        var names = supplierRepository.findAllByOrderByNameAsc().stream()
                .map(Supplier::getName)
                .filter(name -> name.endsWith(suffix))
                .toList();

        assertEquals(2, names.size());
        assertTrue(names.getFirst().startsWith("Alpha"));
        assertTrue(names.getLast().startsWith("Zeta"));
        supplierRepository.deleteById(second.getId());
    }

    @Test
    void rejectsMissingOrBlankSupplierName() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new Supplier(null, null, null)).isEmpty());
            assertFalse(validator.validate(new Supplier("   ", null, null)).isEmpty());
            assertTrue(validator.validate(new Supplier("Valid Supplier", null, null)).isEmpty());
        }
    }
}
