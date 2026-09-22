package com.trichyestates.estatehub.security;

import com.trichyestates.estatehub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The "username" is the e-mail address (case-insensitive). */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
