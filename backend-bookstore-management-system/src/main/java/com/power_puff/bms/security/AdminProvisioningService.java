package com.power_puff.bms.security;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Coordinates the one-time creation of the initial Admin account.
 *
 * <p>A singleton row in SQLite is claimed before the account is inserted.
 * The marker and user row participate in one transaction, so concurrent
 * requests cannot create multiple initial Admins and an interrupted
 * transaction can be retried.</p>
 */
@Service
public class AdminProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminProvisioningService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        if (userRepository.count() > 0) {
            return false;
        }
        Integer completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_bootstrap_state WHERE id = 1",
                Integer.class);
        return completed == null || completed == 0;
    }

    @Transactional
    public User provisionInitialAdmin(String username, String password, String fullName) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedFullName = fullName == null ? "" : fullName.trim();
        validateCredentials(normalizedUsername, password, normalizedFullName);

        // Wait briefly for another local request/process to finish its SQLite write.
        // The singleton INSERT remains the database-level concurrency guard.
        jdbcTemplate.execute("PRAGMA busy_timeout = 5000");
        int claimed = jdbcTemplate.update("""
                INSERT INTO admin_bootstrap_state (id, completed_at)
                VALUES (1, CURRENT_TIMESTAMP)
                ON CONFLICT(id) DO NOTHING
                """);
        if (claimed != 1) {
            throw new DuplicateResourceException(
                    "Initial Admin setup has already been completed.");
        }

        // This check is deliberately after the atomic claim and inside the same
        // transaction. Existing installations can never be reinitialized.
        if (userRepository.count() > 0) {
            throw new DuplicateResourceException(
                    "An account already exists. First-run setup is unavailable.");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException(
                    "The requested username is already in use.");
        }

        User admin = new User(normalizedUsername, passwordEncoder.encode(password),
                normalizedFullName, UserRole.ADMIN);
        return userRepository.saveAndFlush(admin);
    }

    private void validateCredentials(String username, String password, String fullName) {
        if (username.isBlank() || username.length() > 100) {
            throw new IllegalArgumentException(
                    "Username must contain between 1 and 100 characters.");
        }
        if (fullName.isBlank() || fullName.length() > 150) {
            throw new IllegalArgumentException(
                    "Full name must contain between 1 and 150 characters.");
        }
        int passwordBytes = password == null
                ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 8 || passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Password must contain 8 to 72 UTF-8 bytes.");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
