package com.power_puff.bms.schoolorder.service;

import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SchoolOrderStatusServiceTest {

    private final SchoolOrderRepository repository = mock(SchoolOrderRepository.class);
    private final SchoolOrderService service = new SchoolOrderService(
            repository, null, null, null);

    @Test
    void rejectsNullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1, null));
    }

    @Test
    void rejectsUnknownOrder() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateStatus(999999, SchoolOrderStatus.READY));
    }
}
