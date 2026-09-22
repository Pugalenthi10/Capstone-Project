package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.dto.UpdateProfileRequest;
import com.trichyestates.estatehub.dto.UserResponse;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserDetails me) {
        return userService.getProfile(me.getId());
    }

    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest request,
                               @AuthenticationPrincipal AppUserDetails me) {
        return userService.updateProfile(me.getId(), request);
    }
}
