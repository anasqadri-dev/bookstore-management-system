package com.power_puff.bms.user.controller;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.common.response.ApiResponse;
import com.power_puff.bms.user.dto.LoginRequest;
import com.power_puff.bms.user.dto.LoginResponse;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.mapper.UserMapper;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UserRepository userRepository,
                          ActivityLogService activityLogService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username().trim(), request.password()));

        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            servletRequest.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

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
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user account was not found or is inactive."));
    }
}
