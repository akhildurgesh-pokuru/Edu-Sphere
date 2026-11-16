package com.edusphere.auth;

import com.edusphere.auth.AuthDtos.*;
import com.edusphere.common.ApiException;
import com.edusphere.common.TokenUtil;
import com.edusphere.config.AppProperties;
import com.edusphere.domain.PasswordResetToken;
import com.edusphere.mail.EmailService;
import com.edusphere.domain.TeacherInvitation;
import com.edusphere.repository.PasswordResetTokenRepository;
import com.edusphere.repository.TeacherInvitationRepository;
import com.edusphere.user.Role;
import com.edusphere.user.RoleRepository;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final PasswordResetTokenRepository resetTokens;
    private final TeacherInvitationRepository invitations;
    private final AppProperties props;
    private final EmailService emails;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       PasswordResetTokenRepository resetTokens, TeacherInvitationRepository invitations, AppProperties props, EmailService emails) {
        this.users = users; this.roles = roles; this.encoder = encoder;
        this.resetTokens = resetTokens; this.invitations = invitations; this.props = props; this.emails = emails;
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.primaryRole(), u.getPasswordHash() != null, u.isEmailNotifications(), u.getCreatedAt());
    }

    @Transactional
    public User register(RegisterRequest req) {
        String role = "TEACHER".equalsIgnoreCase(req.role()) ? Role.TEACHER : Role.STUDENT;
        if (Role.TEACHER.equals(role) && !props.allowTeacherSignup()) {
            throw ApiException.forbidden("Teacher sign-up is by invitation only");
        }
        User u = create(req.name(), req.email(), req.password(), role);
        emails.sendWelcome(u);
        return u;
    }

    @Transactional
    public User registerTeacher(String token, RegisterRequest req) {
        TeacherInvitation inv = invitations.findByTokenHash(TokenUtil.sha256(token == null ? "" : token))
                .orElseThrow(() -> ApiException.badRequest("This invitation link is invalid or has expired"));
        if (inv.getUsedAt() != null || inv.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This invitation link is invalid or has expired");
        }
        if (!inv.getEmail().equalsIgnoreCase(req.email().trim())) {
            throw ApiException.badRequest("Use the email address the invitation was sent to");
        }
        User u = create(req.name(), req.email(), req.password(), Role.TEACHER);
        inv.setUsedAt(Instant.now());
        invitations.save(inv);
        emails.sendWelcome(u);
        return u;
    }

    public User create(String name, String email, String password, String role) {
        String e = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(e)) throw ApiException.conflict("An account with this email already exists");
        User u = new User();
        u.setEmail(e);
        u.setName(name.trim());
        if (password != null) u.setPasswordHash(encoder.encode(password));
        u.getRoles().add(roles.findByName(role).orElseThrow());
        return users.save(u);
    }

    @Transactional
    public User updateProfile(Long userId, String name, Boolean emailNotifications) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setName(name.trim());
        if (emailNotifications != null) u.setEmailNotifications(emailNotifications);
        u.setUpdatedAt(Instant.now());
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Please sign in to continue"));
    }

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email.trim()).ifPresent(u -> {
            String token = TokenUtil.randomToken();
            PasswordResetToken t = new PasswordResetToken();
            t.setUserId(u.getId());
            t.setTokenHash(TokenUtil.sha256(token));
            t.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            resetTokens.save(t);
            String link = props.frontendUrl() + "/reset-password?token=" + token;
            emails.sendPasswordReset(u, link);
        });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        PasswordResetToken t = resetTokens.findByTokenHash(TokenUtil.sha256(token))
                .orElseThrow(() -> ApiException.badRequest("This reset link is invalid or has expired"));
        if (t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("This reset link is invalid or has expired");
        }
        User u = users.findById(t.getUserId()).orElseThrow(() -> ApiException.badRequest("This reset link is invalid or has expired"));
        u.setPasswordHash(encoder.encode(password));
        u.setUpdatedAt(Instant.now());
        users.save(u);
        t.setUsedAt(Instant.now());
        resetTokens.save(t);
        emails.sendPasswordChanged(u);
    }

    @Transactional
    public InvitationResponse invite(Long teacherId, String email) {
        String token = TokenUtil.randomToken();
        TeacherInvitation inv = new TeacherInvitation();
        inv.setEmail(email.trim().toLowerCase());
        inv.setTokenHash(TokenUtil.sha256(token));
        inv.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        inv.setCreatedBy(teacherId);
        invitations.save(inv);
        String link = props.frontendUrl() + "/register/teacher?token=" + token;
        String inviter = users.findById(teacherId).map(User::getName).orElse("A teacher");
        emails.sendTeacherInvitation(inv.getEmail(), inviter, link);
        return new InvitationResponse(inv.getEmail(), link, inv.getExpiresAt());
    }
}
