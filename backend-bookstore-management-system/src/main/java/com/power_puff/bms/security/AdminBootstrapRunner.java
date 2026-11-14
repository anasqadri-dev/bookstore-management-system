package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "bms.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bms.bootstrap-admin.username:}")
    private String username;

    @Value("${bms.bootstrap-admin.password:}")
    private String password;

    @Value("${bms.bootstrap-admin.full-name:Store Administrator}")
    private String fullName;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Admin bootstrap skipped because at least one user account already exists.");
            return;
        }
        String normalizedUsername = username == null
                ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedFullName = fullName == null ? "" : fullName.trim();
        int passwordBytes = password == null
                ? 0 : password.getBytes(StandardCharsets.UTF_8).length;

        if (normalizedUsername.isBlank() || normalizedUsername.length() > 100) {
            throw new IllegalStateException(
                    "Admin bootstrap requires a username containing 1 to 100 characters.");
        }
        if (normalizedFullName.isBlank() || normalizedFullName.length() > 150) {
            throw new IllegalStateException(
                    "Admin bootstrap requires a full name containing 1 to 150 characters.");
        }
        if (passwordBytes < 8 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "Admin bootstrap password must contain 8 to 72 UTF-8 bytes.");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalStateException(
                    "Admin bootstrap username is already in use; no account was changed.");
        }

        User admin = new User(normalizedUsername, passwordEncoder.encode(password),
                normalizedFullName, UserRole.ADMIN);
        userRepository.saveAndFlush(admin);
        log.info("Initial Admin account created for username '{}'.", admin.getUsername());
    }
}
