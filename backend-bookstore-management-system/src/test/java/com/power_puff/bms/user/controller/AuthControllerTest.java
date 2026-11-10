package com.power_puff.bms.user.controller;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.user.dto.LoginRequest;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulLoginCreatesSessionAndRecordsLogin() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        SecurityContextRepository contextRepository = mock(SecurityContextRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ActivityLogService activityLogService = mock(ActivityLogService.class);

        User staff = new User("staff1", "$2a$encoded", "Staff Member", UserRole.STAFF);
        staff.setId(7L);
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        "staff1", null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        when(userRepository.findByUsernameIgnoreCase("staff1")).thenReturn(Optional.of(staff));

        AuthController controller = new AuthController(
                authenticationManager, contextRepository, userRepository, activityLogService);

        var response = controller.login(new LoginRequest("staff1", "StrongPass123"),
                new MockHttpServletRequest(), new MockHttpServletResponse());

        assertTrue(response.success());
        assertEquals(7L, response.data().id());
        assertEquals(UserRole.STAFF, response.data().role());
        verify(contextRepository).saveContext(any(), any(), any());
        verify(activityLogService).record(staff, "LOGIN", "Successful login");
    }

    @Test
    void invalidCredentialsAreRejected() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AuthController controller = new AuthController(
                authenticationManager,
                mock(SecurityContextRepository.class),
                mock(UserRepository.class),
                mock(ActivityLogService.class));

        assertThrows(BadCredentialsException.class, () -> controller.login(
                new LoginRequest("staff1", "wrong-password"),
                new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test
    void logoutInvalidatesExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextRepository contextRepository = mock(SecurityContextRepository.class);

        AuthController controller = new AuthController(
                mock(AuthenticationManager.class), contextRepository,
                mock(UserRepository.class), mock(ActivityLogService.class));

        var result = controller.logout(request, response);

        assertTrue(result.success());
        assertNull(request.getSession(false));
        verify(contextRepository).saveContext(any(), eq(request), eq(response));
    }
}
