package com.edusphere.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String p = req.getRequestURI();
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        res.setHeader("Cross-Origin-Resource-Policy", "same-site");
        if (p.startsWith("/api/") && !p.startsWith("/api/resources/") && !p.contains("/cover")) {
            res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
            res.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(req, res);
    }
}
