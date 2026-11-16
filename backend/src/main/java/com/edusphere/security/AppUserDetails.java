package com.edusphere.security;

import com.edusphere.user.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String email;
    private final String name;
    private final String passwordHash;
    private final boolean enabled;
    private final String role;

    public AppUserDetails(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.name = u.getName();
        this.passwordHash = u.getPasswordHash() == null ? "!" : u.getPasswordHash();
        this.enabled = u.isEnabled();
        this.role = u.primaryRole();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isTeacher() { return "TEACHER".equals(role); }
    public boolean isStudent() { return "STUDENT".equals(role); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
}
