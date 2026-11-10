package com.power_puff.bms.user.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String username, String passwordHash, String fullName, UserRole role) {
        setUsername(username);
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
    }

    @PrePersist
    private void beforeInsert() {
        normalizeUsername();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    private void beforeUpdate() {
        normalizeUsername();
    }

    private void normalizeUsername() {
        if (username != null) {
            username = username.trim().toLowerCase(Locale.ROOT);
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(UserRole role) { this.role = role; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
