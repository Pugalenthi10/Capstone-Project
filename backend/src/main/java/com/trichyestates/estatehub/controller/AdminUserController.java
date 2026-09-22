package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.PageResponse;
import com.trichyestates.estatehub.dto.UpdateRoleRequest;
import com.trichyestates.estatehub.dto.UserResponse;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin-only user and agent management (promote a user to AGENT, etc.). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return userService.listUsers(page, size);
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request,
                                   @AuthenticationPrincipal AppUserDetails me) {
        return userService.changeRole(me.getId(), id, request.role());
    }
}
