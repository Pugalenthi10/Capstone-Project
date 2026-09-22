package com.trichyestates.estatehub.security;

import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Authenticated principal. Carries the user id so controllers never trust an id sent by the client. */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPassword();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
