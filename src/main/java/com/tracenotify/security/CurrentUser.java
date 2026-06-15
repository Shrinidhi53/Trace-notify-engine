package com.tracenotify.security;

import com.tracenotify.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ApiException(401, "Unauthenticated");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }
}
