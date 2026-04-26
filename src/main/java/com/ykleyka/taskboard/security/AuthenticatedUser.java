package com.ykleyka.taskboard.security;

import com.ykleyka.taskboard.model.User;

public record AuthenticatedUser(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName());
    }
}
