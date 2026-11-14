package com.power_puff.bms.user.service;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.user.dto.CreateStaffRequest;
import com.power_puff.bms.user.dto.UpdateStaffRequest;
import com.power_puff.bms.user.dto.UserResponse;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.mapper.UserMapper;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserAccountService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    public UserResponse createStaff(CreateStaffRequest request) {
        String username = normalizeUsername(request.username());
        ensureUsernameAvailable(username);
        validateBcryptPasswordLength(request.password());

        User user = new User(username, passwordEncoder.encode(request.password()),
                request.fullName().trim(), UserRole.STAFF);
        User saved = userRepository.save(user);
        activityLogService.recordCurrentUserAction(
                "STAFF_CREATED", "Created Staff account id=" + saved.getId()
                        + ", username=" + saved.getUsername());
        return UserMapper.toResponse(saved);
    }

    public UserResponse updateStaff(Long id, UpdateStaffRequest request) {
        User user = getManagedStaff(id);
        boolean nameChanged = !user.getFullName().equals(request.fullName().trim());
        boolean wasActive = user.isActive();
        boolean passwordChanged = request.newPassword() != null && !request.newPassword().isBlank();
        user.setFullName(request.fullName().trim());

        if (passwordChanged) {
            validateBcryptPasswordLength(request.newPassword());
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        User saved = userRepository.save(user);
        if (passwordChanged) {
            activityLogService.recordCurrentUserAction(
                    "STAFF_PASSWORD_CHANGED", "Changed password for Staff account id="
                            + saved.getId() + ", username=" + saved.getUsername());
        }
        if (wasActive && !saved.isActive()) {
            activityLogService.recordCurrentUserAction(
                    "STAFF_DEACTIVATED", "Deactivated Staff account id="
                            + saved.getId() + ", username=" + saved.getUsername());
        }
        if (nameChanged || (!wasActive && saved.isActive())) {
            activityLogService.recordCurrentUserAction(
                    "STAFF_UPDATED", "Updated Staff account id="
                            + saved.getId() + ", username=" + saved.getUsername());
        }
        return UserMapper.toResponse(saved);
    }

    public UserResponse deactivateStaff(Long id) {
        User user = getManagedStaff(id);
        boolean wasActive = user.isActive();
        user.setActive(false);
        User saved = userRepository.save(user);
        if (wasActive) {
            activityLogService.recordCurrentUserAction(
                    "STAFF_DEACTIVATED", "Deactivated Staff account id="
                            + saved.getId() + ", username=" + saved.getUsername());
        }
        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public User findActiveUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user account was not found or is inactive."));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByUsernameAsc()
                .stream().map(UserMapper::toResponse).toList();
    }

    private User getManagedStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + id + " was not found."));
        if (user.getRole() != UserRole.STAFF) {
            throw new AccessDeniedException("Only Staff accounts can be managed here.");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("A user with this username already exists.");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
    }
}
