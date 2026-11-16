package com.edusphere.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.edusphere.auth.AuthDtos.RegisterRequest;
import com.edusphere.common.ApiException;
import com.edusphere.config.AppProperties;
import com.edusphere.domain.PasswordResetToken;
import com.edusphere.domain.TeacherInvitation;
import com.edusphere.repository.PasswordResetTokenRepository;
import com.edusphere.repository.TeacherInvitationRepository;
import com.edusphere.user.Role;
import com.edusphere.user.RoleRepository;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock TeacherInvitationRepository invitations;
    @Mock com.edusphere.mail.EmailService emails;
    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService service;

    private AuthService serviceWith(boolean teacherSignup) {
        AppProperties props = new AppProperties("http://localhost:5173", "http://localhost:8080", "./uploads", false, teacherSignup,
                new AppProperties.Mail(false, "no-reply@test", "EduSphere"), new AppProperties.BootstrapTeacher("", "", ""));
        return new AuthService(users, roles, encoder, resetTokens, invitations, props, emails);
    }

    @BeforeEach
    void setUp() {
        service = serviceWith(true);
        Role student = new Role(); student.setName(Role.STUDENT);
        Role teacher = new Role(); teacher.setName(Role.TEACHER);
        when(roles.findByName(Role.STUDENT)).thenReturn(Optional.of(student));
        when(roles.findByName(Role.TEACHER)).thenReturn(Optional.of(teacher));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registersAStudentWithAHashedPasswordAndLowercaseEmail() {
        User u = service.register(new RegisterRequest("Ada", "  Ada@Example.COM ", "Passw0rdX", "STUDENT"));
        assertThat(u.getEmail()).isEqualTo("ada@example.com");
        assertThat(u.getPasswordHash()).startsWith("$2").isNotEqualTo("Passw0rdX");
        assertThat(encoder.matches("Passw0rdX", u.getPasswordHash())).isTrue();
        assertThat(u.primaryRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void registersATeacherWhenSignupIsAllowed() {
        assertThat(service.register(new RegisterRequest("T", "t@x.com", "Passw0rdX", "TEACHER")).primaryRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    void anythingElseBecomesAStudent() {
        assertThat(service.register(new RegisterRequest("T", "t@x.com", "Passw0rdX", "ADMIN")).primaryRole()).isEqualTo(Role.STUDENT);
        assertThat(service.register(new RegisterRequest("T", "t2@x.com", "Passw0rdX", null)).primaryRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void teacherSignupCanBeSwitchedOff() {
        AuthService off = serviceWith(false);
        assertThatThrownBy(() -> off.register(new RegisterRequest("T", "t@x.com", "Passw0rdX", "TEACHER")))
                .isInstanceOf(ApiException.class).hasMessageContaining("invitation");
        assertThat(off.register(new RegisterRequest("S", "s@x.com", "Passw0rdX", "STUDENT")).primaryRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void rejectsDuplicateEmails() {
        when(users.existsByEmailIgnoreCase("dup@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("D", "dup@x.com", "Passw0rdX", "STUDENT")))
                .isInstanceOf(ApiException.class).hasMessageContaining("already exists");
        verify(users, never()).save(any());
    }

    @Test
    void invitationMustBeValidUnusedUnexpiredAndForTheSameEmail() {
        TeacherInvitation inv = new TeacherInvitation();
        inv.setEmail("invited@x.com");
        inv.setExpiresAt(Instant.now().plusSeconds(3600));
        when(invitations.findByTokenHash(any())).thenReturn(Optional.of(inv));
        assertThatThrownBy(() -> service.registerTeacher("tok", new RegisterRequest("X", "other@x.com", "Passw0rdX", "TEACHER"))).hasMessageContaining("invitation was sent to");
        inv.setUsedAt(Instant.now());
        assertThatThrownBy(() -> service.registerTeacher("tok", new RegisterRequest("X", "invited@x.com", "Passw0rdX", "TEACHER"))).hasMessageContaining("invalid or has expired");
        inv.setUsedAt(null);
        inv.setExpiresAt(Instant.now().minusSeconds(5));
        assertThatThrownBy(() -> service.registerTeacher("tok", new RegisterRequest("X", "invited@x.com", "Passw0rdX", "TEACHER"))).hasMessageContaining("invalid or has expired");
        when(invitations.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registerTeacher("nope", new RegisterRequest("X", "invited@x.com", "Passw0rdX", "TEACHER"))).hasMessageContaining("invalid or has expired");
    }

    @Test
    void resetTokenIsStoredOnlyAsAHash() {
        User u = new User(); u.setId(5L); u.setEmail("a@x.com");
        when(users.findByEmailIgnoreCase("a@x.com")).thenReturn(Optional.of(u));
        service.forgotPassword("a@x.com");
        ArgumentCaptor<PasswordResetToken> c = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokens).save(c.capture());
        assertThat(c.getValue().getTokenHash()).hasSize(64).matches("[0-9a-f]+");
        assertThat(c.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void forgotPasswordForUnknownEmailLeaksNothing() {
        when(users.findByEmailIgnoreCase("ghost@x.com")).thenReturn(Optional.empty());
        service.forgotPassword("ghost@x.com");
        verify(resetTokens, never()).save(any());
    }

    @Test
    void usedOrExpiredResetTokensAreRejected() {
        PasswordResetToken t = new PasswordResetToken();
        t.setUserId(1L);
        t.setExpiresAt(Instant.now().plusSeconds(60));
        t.setUsedAt(Instant.now());
        when(resetTokens.findByTokenHash(any())).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.resetPassword("tok", "NewPassw0rd")).isInstanceOf(ApiException.class);
        t.setUsedAt(null);
        t.setExpiresAt(Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> service.resetPassword("tok", "NewPassw0rd")).isInstanceOf(ApiException.class);
    }

    @Test
    void forgotPasswordEmailsTheResetLinkOnlyForKnownUsers() {
        User u = new User(); u.setId(5L); u.setEmail("a@b.com"); u.setName("A");
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(u));
        when(users.findByEmailIgnoreCase("nobody@b.com")).thenReturn(Optional.empty());
        service.forgotPassword("a@b.com");
        service.forgotPassword("nobody@b.com");
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emails, times(1)).sendPasswordReset(org.mockito.ArgumentMatchers.eq(u), link.capture());
        assertThat(link.getValue()).startsWith("http://localhost:5173/reset-password?token=");
    }
}
