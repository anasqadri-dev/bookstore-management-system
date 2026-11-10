package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsServiceTest {

    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void loadsActiveStaffWithStaffAuthority() {
        UserRepository repository = mock(UserRepository.class);
        User staff = new User("staff1", "$2a$encoded", "Staff Member", UserRole.STAFF);
        when(repository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(staff));

        var details = configuration.userDetailsService(repository).loadUserByUsername("staff1");

        assertEquals("staff1", details.getUsername());
        assertEquals("$2a$encoded", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")));
        assertTrue(details.isEnabled());
    }

    @Test
    void rejectsInactiveAccountDuringAuthenticationLookup() {
        UserRepository repository = mock(UserRepository.class);
        User inactive = new User("staff2", "$2a$encoded", "Inactive", UserRole.STAFF);
        inactive.setActive(false);
        when(repository.findByUsernameIgnoreCase("staff2")).thenReturn(Optional.of(inactive));

        assertThrows(UsernameNotFoundException.class,
                () -> configuration.userDetailsService(repository).loadUserByUsername("staff2"));
    }
}
