package com.trichyestates.estatehub.dto;

import com.trichyestates.estatehub.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull(message = "Role is required") Role role) { }
