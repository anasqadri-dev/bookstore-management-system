package com.power_puff.bms.security;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AdminBootstrapRunnerTest {

    @Test
    void delegatesExplicitBootstrapToAtomicProvisioningService() {
        AdminProvisioningService service = mock(AdminProvisioningService.class);
        when(service.provisionInitialAdmin("owner", "OwnerPassword123", "Store Owner"))
                .thenReturn(new User("owner", "encoded", "Store Owner", UserRole.ADMIN));

        AdminBootstrapRunner runner = runner(service,
                " owner ", "OwnerPassword123", " Store Owner ");

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(service).provisionInitialAdmin(
                " owner ", "OwnerPassword123", " Store Owner ");
    }

    @Test
    void skipsWhenFirstAdminHasAlreadyBeenProvisioned() {
        AdminProvisioningService service = mock(AdminProvisioningService.class);
        when(service.provisionInitialAdmin(anyString(), anyString(), anyString()))
                .thenThrow(new DuplicateResourceException("already initialized"));

        AdminBootstrapRunner runner = runner(service, "owner", "OwnerPassword123", "Owner");

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(service).provisionInitialAdmin("owner", "OwnerPassword123", "Owner");
    }

    @Test
    void propagatesInvalidOperatorCredentialsWithoutLoggingThem() {
        AdminProvisioningService service = mock(AdminProvisioningService.class);
        when(service.provisionInitialAdmin(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("weak password"));

        AdminBootstrapRunner runner = runner(service, "owner", "weak", "Owner");

        assertThrows(IllegalArgumentException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
    }

    private AdminBootstrapRunner runner(AdminProvisioningService service,
                                        String username,
                                        String password,
                                        String fullName) {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(service);
        ReflectionTestUtils.setField(runner, "username", username);
        ReflectionTestUtils.setField(runner, "password", password);
        ReflectionTestUtils.setField(runner, "fullName", fullName);
        return runner;
    }
}
