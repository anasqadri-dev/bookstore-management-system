package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapRunnerTest {

    @Test
    void createsExactlyOneAdminWithEncodedPasswordOnFreshDatabase() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.count()).thenReturn(0L);
        when(repository.existsByUsernameIgnoreCase("owner")).thenReturn(false);
        when(encoder.encode("OwnerPassword123")).thenReturn("encoded-password");
        when(repository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminBootstrapRunner runner = runner(repository, encoder,
                " Owner ", "OwnerPassword123", " Store Owner ");

        runner.run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertEquals("owner", saved.getUsername());
        assertEquals("encoded-password", saved.getPasswordHash());
        assertEquals("Store Owner", saved.getFullName());
        assertEquals(UserRole.ADMIN, saved.getRole());
        verify(repository).count();
    }

    @Test
    void skipsBootstrapWhenAnyAccountAlreadyExists() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.count()).thenReturn(1L);
        AdminBootstrapRunner runner = runner(repository, mock(PasswordEncoder.class),
                "", "", "");

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(repository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsMissingOrWeakCredentialsWithoutCreatingAnAccount() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.count()).thenReturn(0L);
        AdminBootstrapRunner runner = runner(repository, mock(PasswordEncoder.class),
                "owner", "short", "Store Owner");

        assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(new String[0])));
        verify(repository, never()).saveAndFlush(any(User.class));
    }

    private AdminBootstrapRunner runner(UserRepository repository,
                                        PasswordEncoder encoder,
                                        String username,
                                        String password,
                                        String fullName) {
        AdminBootstrapRunner runner = new AdminBootstrapRunner(repository, encoder);
        ReflectionTestUtils.setField(runner, "username", username);
        ReflectionTestUtils.setField(runner, "password", password);
        ReflectionTestUtils.setField(runner, "fullName", fullName);
        return runner;
    }
}
