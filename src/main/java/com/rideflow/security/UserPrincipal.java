package com.rideflow.security;

import com.rideflow.user.Role;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticated user principal stored in Spring Security's SecurityContext.
 */
public class UserPrincipal implements Principal {

    private final UUID id;
    private final String email;
    private final Role role;

    public UserPrincipal(UUID id, String email, Role role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    @Override
    public String getName() {
        return email;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public static UserPrincipal from(com.rideflow.user.User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getRole());
    }

    public static UserPrincipal create(com.rideflow.user.User user) {
        return from(user);
    }
}
