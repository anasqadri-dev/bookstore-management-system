package com.power_puff.bms.activitylog.service;

import com.power_puff.bms.activitylog.entity.ActivityLog;
import com.power_puff.bms.activitylog.repository.ActivityLogRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivityLogServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsActionForAuthenticatedUser() {
        ActivityLogRepository logRepository = mock(ActivityLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User("admin", "encoded", "Store Owner", UserRole.ADMIN);
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        new ActivityLogService(logRepository, userRepository)
                .recordCurrentUserAction("DELETE", "Deleted product id=5");

        verify(logRepository).save(argThat(log ->
                log.getUser().equals(user)
                        && log.getAction().equals("DELETE")
                        && log.getDetails().contains("product id=5")));
    }

    @Test
    void doesNotRecordActionForAnonymousRequest() {
        ActivityLogRepository logRepository = mock(ActivityLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        SecurityContextHolder.clearContext();

        new ActivityLogService(logRepository, userRepository)
                .recordCurrentUserAction("DELETE", "attempt");

        verifyNoInteractions(logRepository, userRepository);
    }
}
