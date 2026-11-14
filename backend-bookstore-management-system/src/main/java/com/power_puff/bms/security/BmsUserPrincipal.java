package com.power_puff.bms.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

/**
 * Session principal that retains only a SHA-256 fingerprint of the password
 * hash after Spring Security erases credentials from the authentication.
 */
public final class BmsUserPrincipal implements UserDetails, CredentialsContainer {

    private final Long userId;
    private final String username;
    private final String role;
    private final String passwordHashFingerprint;
    private String password;

    public BmsUserPrincipal(Long userId, String username, String passwordHash, String role) {
        this.userId = userId;
        this.username = username;
        this.password = passwordHash;
        this.role = role;
        this.passwordHashFingerprint = fingerprint(passwordHash);
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean matchesCurrentPasswordHash(String currentPasswordHash) {
        return MessageDigest.isEqual(
                passwordHashFingerprint.getBytes(StandardCharsets.US_ASCII),
                fingerprint(currentPasswordHash).getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }

    private static String fingerprint(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
