package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;

/** Public view of a user. Deliberately has no password field. */
public record UserResponse(Long id, String name, String email, String mobile, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getMobile(), u.getRole());
    }
}
