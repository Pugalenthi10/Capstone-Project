package com.trichyestates.estatehub.util;

import com.trichyestates.estatehub.security.AppUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() { }

    /** The authenticated user, taken from the verified JWT - never from request data. */
    public static AppUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new AccessDeniedException("Not authenticated");
        }
        return details;
    }
}
