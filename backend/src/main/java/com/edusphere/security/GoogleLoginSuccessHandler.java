package com.edusphere.security;

import com.edusphere.auth.GoogleAccountService;
import com.edusphere.config.AppProperties;
import com.edusphere.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(GoogleLoginSuccessHandler.class);
    private final GoogleAccountService accounts;
    private final AppProperties props;
    private final HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();

    public GoogleLoginSuccessHandler(GoogleAccountService accounts, AppProperties props) {
        this.accounts = accounts;
        this.props = props;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        try {
            if (!(auth.getPrincipal() instanceof OidcUser oidc)) throw new IllegalStateException("Not an OIDC user");
            boolean verified = Boolean.TRUE.equals(oidc.getEmailVerified());
            User user = accounts.loginOrCreate(oidc.getSubject(), oidc.getEmail(), verified, oidc.getFullName());
            AppUserDetails details = new AppUserDetails(user);
            var token = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(token);
            if (req.getSession(false) != null) req.changeSessionId();
            repo.saveContext(ctx, req, res);
            res.sendRedirect(props.frontendUrl() + "/dashboard");
        } catch (Exception e) {
            log.warn("Google sign-in rejected: {}", e.getMessage());
            res.sendRedirect(props.frontendUrl() + "/login?error=google");
        }
    }
}
