package com.fsmonitor.app.dto;

import com.fsmonitor.app.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/** API representation of a user - never carries the password hash. */
public record UserResponse(
        Long id,
        String username,
        String email,
        Boolean passwordChanged,
        Boolean mustChangePassword,
        Set<String> roles,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles() == null ? Set.of() :
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordChanged(),
                user.getMustChangePassword(),
                roleNames,
                user.getCreatedAt());
    }
}
