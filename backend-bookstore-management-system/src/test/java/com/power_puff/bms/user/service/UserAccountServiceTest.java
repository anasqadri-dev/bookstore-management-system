package com.power_puff.bms.user.service;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.user.dto.CreateStaffRequest;
import com.power_puff.bms.user.dto.UpdateStaffRequest;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    void createsStaffWithHashedPasswordAndStaffRole() {
        when(userRepository.existsByUsernameIgnoreCase("counter1")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });

        var response = userAccountService.createStaff(
                new CreateStaffRequest("Counter1", "SecurePass123", "Counter Worker"));

        assertEquals(11L, response.id());
        assertEquals(UserRole.STAFF, response.role());
        assertEquals("counter1", response.username());
        verify(passwordEncoder).encode("SecurePass123");
        verify(userRepository).save(argThat(user ->
                user.getPasswordHash().equals("$2a$hashed-value")
                        && !user.getPasswordHash().equals("SecurePass123")
                        && user.getRole() == UserRole.STAFF
                        && user.isActive()));
        verify(activityLogService).recordCurrentUserAction(
                "STAFF_CREATED", "Created Staff account id=11, username=counter1");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("counter1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userAccountService.createStaff(
                        new CreateStaffRequest("Counter1", "SecurePass123", "Counter Worker")));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivatesStaffInsteadOfDeletingHistoricalAccount() {
        User staff = new User("counter1", "encoded", "Counter Worker", UserRole.STAFF);
        staff.setId(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(staff));
        when(userRepository.save(staff)).thenReturn(staff);

        var response = userAccountService.deactivateStaff(11L);

        assertFalse(response.active());
        verify(userRepository, never()).delete(any(User.class));
        verify(activityLogService).recordCurrentUserAction(
                "STAFF_DEACTIVATED", "Deactivated Staff account id=11, username=counter1");
    }

    @Test
    void recordsPasswordChangesWithoutIncludingCredentialMaterial() {
        User staff = new User("counter1", "old-hash", "Counter Worker", UserRole.STAFF);
        staff.setId(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(staff));
        when(passwordEncoder.encode("NewSecurePass123")).thenReturn("new-hash");
        when(userRepository.save(staff)).thenReturn(staff);

        userAccountService.updateStaff(
                11L, new UpdateStaffRequest("Counter Worker", "NewSecurePass123", null));

        verify(activityLogService).recordCurrentUserAction(
                "STAFF_PASSWORD_CHANGED",
                "Changed password for Staff account id=11, username=counter1");
    }

    @Test
    void preventsManagingAdminThroughStaffEndpoints() {
        User admin = new User("admin", "encoded", "Store Owner", UserRole.ADMIN);
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class,
                () -> userAccountService.updateStaff(
                        1L, new UpdateStaffRequest("Changed", null, null)));
    }
}
