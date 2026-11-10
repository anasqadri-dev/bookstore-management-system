package com.power_puff.bms.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfiguration().passwordEncoder();

    @Test
    void hashesPasswordAndVerifiesOnlyMatchingRawPassword() {
        String rawPassword = "StrongLocalPassword123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("incorrect", encodedPassword));
    }
}
