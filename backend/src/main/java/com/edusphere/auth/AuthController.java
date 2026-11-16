package com.edusphere.auth;

import com.edusphere.auth.AuthDtos.*;
import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.config.AppProperties;
import com.edusphere.security.AppUserDetails;
import com.edusphere.security.LoginRateLimiter;
import com.edusphere.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Auth")
public class AuthController {
    private final AuthService auth;
    private final AuthenticationManager manager;
    private final SecurityContextRepository ctxRepo;
    private final LoginRateLimiter limiter;
    private final AppProperties props;

    public AuthController(AuthService auth, AuthenticationManager manager, SecurityContextRepository ctxRepo,
                          LoginRateLimiter limiter, AppProperties props) {
        this.auth = auth; this.manager = manager; this.ctxRepo = ctxRepo; this.limiter = limiter; this.props = props;
    }

    @GetMapping("/auth/csrf")
    @Operation(summary = "Sets the XSRF-TOKEN cookie")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/auth/providers")
    @Operation(summary = "Which sign-in options are enabled")
    public ProvidersResponse providers() {
        return new ProvidersResponse(props.googleEnabled(), props.allowTeacherSignup());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register as STUDENT or TEACHER and sign in")
    public UserResponse register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http, HttpServletResponse res) {
        return signIn(auth.register(req), http, res);
    }

    @PostMapping("/auth/register-teacher")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register as a teacher through an invitation link")
    public UserResponse registerTeacher(@RequestParam String token, @Valid @RequestBody RegisterRequest req,
                                        HttpServletRequest http, HttpServletResponse res) {
        return signIn(auth.registerTeacher(token, req), http, res);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Sign in with email and password")
    public UserResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http, HttpServletResponse res) {
        String key = http.getRemoteAddr() + "|" + req.email().trim().toLowerCase();
        limiter.checkAllowed(key);
        try {
            Authentication a = manager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
            limiter.reset(key);
            return signIn(auth.get(((AppUserDetails) a.getPrincipal()).getId()), http, res);
        } catch (AuthenticationException e) {
            limiter.recordFailure(key);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    private UserResponse signIn(User user, HttpServletRequest http, HttpServletResponse res) {
        AppUserDetails details = new AppUserDetails(user);
        Authentication token = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(token);
        SecurityContextHolder.setContext(ctx);
        if (http.getSession(false) != null) http.changeSessionId(); else http.getSession(true);
        ctxRepo.saveContext(ctx, http, res);
        return AuthService.toResponse(user);
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Sign out and invalidate the session")
    public Map<String, String> logout(HttpServletRequest http) {
        var s = http.getSession(false);
        if (s != null) s.invalidate();
        SecurityContextHolder.clearContext();
        return Map.of("message", "Signed out");
    }

    @GetMapping("/auth/me")
    @Operation(summary = "Current user")
    public UserResponse me() {
        return AuthService.toResponse(auth.get(CurrentUser.id()));
    }

    @PutMapping("/auth/profile")
    @Operation(summary = "Update the display name")
    public UserResponse profile(@Valid @RequestBody ProfileRequest req) {
        return AuthService.toResponse(auth.updateProfile(CurrentUser.id(), req.name(), req.emailNotifications()));
    }

    @PostMapping("/auth/forgot-password")
    @Operation(summary = "Request a password reset link (always answers the same)")
    public Map<String, String> forgot(@Valid @RequestBody ForgotRequest req, HttpServletRequest http) {
        String key = "forgot|" + http.getRemoteAddr();
        limiter.checkAllowed(key);
        limiter.recordFailure(key);
        auth.forgotPassword(req.email());
        return Map.of("message", "If that email has an account, a reset link has been sent.");
    }

    @PostMapping("/auth/reset-password")
    @Operation(summary = "Set a new password with a single-use token")
    public Map<String, String> reset(@Valid @RequestBody ResetRequest req) {
        auth.resetPassword(req.token(), req.password());
        return Map.of("message", "Password updated. You can sign in now.");
    }

    @PostMapping("/teacher/invitations")
    @Operation(summary = "Create a teacher invitation link")
    public InvitationResponse invite(@Valid @RequestBody InvitationRequest req) {
        return auth.invite(CurrentUser.id(), req.email());
    }
}
