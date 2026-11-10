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
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Admin bootstrap skipped because at least one user account already exists.");
            return;
        }
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled but BMS_BOOTSTRAP_ADMIN_USERNAME or " +
                    "BMS_BOOTSTRAP_ADMIN_PASSWORD is missing.");
        }

        User admin = new User(username, passwordEncoder.encode(password),
                fullName, UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Initial Admin account created for username '{}'.", admin.getUsername());
    }
}
