package com.rideflow.auth;

import com.rideflow.user.Role;

import java.util.UUID;

/**
 * Safe public representation of a user profile returned after registration.
 * Does not expose passwords, hashes, or internal secrets.
 */
public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private boolean active;

    public UserResponse() {
    }

    public UserResponse(UUID id, String fullName, String email, Role role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
