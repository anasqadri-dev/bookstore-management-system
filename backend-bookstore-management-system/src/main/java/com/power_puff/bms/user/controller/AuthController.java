package com.power_puff.bms.user.controller;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.user.dto.LoginRequest;
import com.power_puff.bms.user.dto.LoginResponse;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.mapper.UserMapper;
import com.power_puff.bms.user.service.UserAccountService;
import com.power_puff.bms.security.ActiveSessionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountService userAccountService;
    private final ActivityLogService activityLogService;
    private final ActiveSessionRegistry activeSessionRegistry;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UserAccountService userAccountService,
                          ActivityLogService activityLogService,
                          ActiveSessionRegistry activeSessionRegistry) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userAccountService = userAccountService;
        this.activityLogService = activityLogService;
        this.activeSessionRegistry = activeSessionRegistry;
    }

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrfToken(CsrfToken csrfToken) {
        return ApiResponse.success(
                "CSRF token generated",
                Map.of("token", csrfToken.getToken()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim(), request.password()));
        } catch (AuthenticationException authenticationFailure) {
            try {
                activityLogService.recordAuthenticationFailure(request.username());
            } catch (RuntimeException auditFailure) {
                // Authentication behavior must remain deterministic if audit storage fails.
                // The warning intentionally excludes the attempted username and credentials.
                log.warn("Could not persist a failed-login audit event.", auditFailure);
            }
            throw authenticationFailure;
        }

        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            servletRequest.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        activeSessionRegistry.register(servletRequest.getSession(true));

        User user = findAuthenticatedUser(authentication);
        activityLogService.record(user, "LOGIN", "Successful login");
        return ApiResponse.success("Login successful", UserMapper.toLoginResponse(user));
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = findAuthenticatedUser(authentication);
        return ApiResponse.success("Current user retrieved successfully",
                UserMapper.toLoginResponse(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            activeSessionRegistry.unregister(session);
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
        securityContextRepository.saveContext(
                SecurityContextHolder.createEmptyContext(), servletRequest, servletResponse);
        return ApiResponse.success("Logout successful");
    }

    private User findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user account was not found.");
        }
        return userAccountService.findActiveUserByUsername(authentication.getName());
    }
}
