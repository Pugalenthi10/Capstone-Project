package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.dto.AuthResponse;
import com.trichyestates.estatehub.dto.LoginRequest;
import com.trichyestates.estatehub.dto.RegisterRequest;
import com.trichyestates.estatehub.dto.UserResponse;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.exception.DuplicateResourceException;
import com.trichyestates.estatehub.repository.UserRepository;
import com.trichyestates.estatehub.security.AppUserDetails;
import com.trichyestates.estatehub.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        String mobile = req.mobile().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("email", "An account with this email already exists");
        }
        if (userRepository.existsByMobile(mobile)) {
            throw new DuplicateResourceException("mobile", "An account with this mobile number already exists");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);   // self-registration can only ever create a normal user

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            // Two simultaneous sign-ups: the DB unique constraints are the final safety net.
            throw new DuplicateResourceException("email", "An account with these details already exists");
        }
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizeEmail(req.email()), req.password()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Same message for "unknown email" and "wrong password": no account enumeration.
            throw new BadCredentialsException("Invalid email or password");
        }
        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
