package com.edusphere.common;

import com.edusphere.security.AppUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static AppUserDetails get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AppUserDetails u)) {
            throw ApiException.unauthorized("Please sign in to continue");
        }
        return u;
    }

    public static Long id() { return get().getId(); }
}
