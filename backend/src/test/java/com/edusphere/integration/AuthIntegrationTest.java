package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registrationValidationReturnsFieldErrors() throws Exception {
        post(null, "/api/auth/register", Map.of("name", "", "email", "nope", "password", "abc", "role", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void registerStudentAndTeacherGetTheRequestedRole() throws Exception {
        assertThat(student().user().get("role").asText()).isEqualTo("STUDENT");
        assertThat(teacher().user().get("role").asText()).isEqualTo("TEACHER");
    }

    @Test
    void unknownRoleCannotBecomeAnythingPrivileged() throws Exception {
        String email = uniqueEmail("admin");
        post(null, "/api/auth/register", Map.of("name", "X", "email", email, "password", PASSWORD, "role", "ADMIN"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        Sess s = student();
        post(null, "/api/auth/register", Map.of("name", "Dup", "email", s.user().get("email").asText().toUpperCase(), "password", PASSWORD, "role", "STUDENT"))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        Sess s = student();
        String hash = jdbc.queryForObject("select password_hash from users where id = ?", String.class, s.id());
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void loginFailureGivesAGenericMessage() throws Exception {
        Sess s = student();
        String email = s.user().get("email").asText();
        post(null, "/api/auth/login", Map.of("email", email, "password", "WrongPass1")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
        post(null, "/api/auth/login", Map.of("email", "ghost@test.local", "password", "WrongPass1")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginWorksAndMeReturnsTheUser() throws Exception {
        Sess s = student();
        var res = post(null, "/api/auth/login", Map.of("email", s.user().get("email").asText(), "password", PASSWORD)).andExpect(status().isOk()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) res.getRequest().getSession();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(s.user().get("email").asText()));
    }

    @Test
    void loginIsRateLimitedAfterRepeatedFailures() throws Exception {
        String email = uniqueEmail("ratelimit");
        for (int i = 0; i < 5; i++) post(null, "/api/auth/login", Map.of("email", email, "password", "WrongPass1")).andExpect(status().isUnauthorized());
        post(null, "/api/auth/login", Map.of("email", email, "password", "WrongPass1")).andExpect(status().isTooManyRequests());
    }

    @Test
    void protectedEndpointsNeedASession() throws Exception {
        get(null, "/api/auth/me").andExpect(status().isUnauthorized());
        get(null, "/api/student/subjects").andExpect(status().isUnauthorized());
        get(null, "/api/teacher/subjects").andExpect(status().isUnauthorized());
        get(null, "/api/notifications").andExpect(status().isUnauthorized());
    }

    @Test
    void writesWithoutCsrfTokenAreForbidden() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        Sess t = teacher();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/teacher/subjects").session(t.session()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        Sess s = student();
        get(s, "/api/auth/me").andExpect(status().isOk());
        post(s, "/api/auth/logout", null).andExpect(status().isOk());
        get(s, "/api/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetTokenIsSingleUse() throws Exception {
        Sess s = student();
        String email = s.user().get("email").asText();
        post(null, "/api/auth/forgot-password", Map.of("email", email)).andExpect(status().isOk());
        // the raw token only ever exists in the log, so create a known one directly (stored hashed, like the service does)
        String raw = com.edusphere.common.TokenUtil.randomToken();
        jdbc.update("insert into password_reset_tokens (user_id, token_hash, expires_at) values (?, ?, date_add(utc_timestamp(), interval 1 hour))", s.id(), com.edusphere.common.TokenUtil.sha256(raw));
        post(null, "/api/auth/reset-password", Map.of("token", raw, "password", "BrandNew123")).andExpect(status().isOk());
        post(null, "/api/auth/reset-password", Map.of("token", raw, "password", "AnotherOne123")).andExpect(status().isBadRequest());
        post(null, "/api/auth/login", Map.of("email", email, "password", "BrandNew123")).andExpect(status().isOk());
        post(null, "/api/auth/login", Map.of("email", email, "password", PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        Sess s = student();
        String raw = com.edusphere.common.TokenUtil.randomToken();
        jdbc.update("insert into password_reset_tokens (user_id, token_hash, expires_at) values (?, ?, date_sub(utc_timestamp(), interval 1 hour))", s.id(), com.edusphere.common.TokenUtil.sha256(raw));
        post(null, "/api/auth/reset-password", Map.of("token", raw, "password", "BrandNew123")).andExpect(status().isBadRequest());
    }

    @Test
    void teacherInvitationCreatesATeacherOnceForTheInvitedEmail() throws Exception {
        Sess t = teacher();
        String email = uniqueEmail("invited");
        String link = body(post(t, "/api/teacher/invitations", Map.of("email", email)).andExpect(status().isOk())).get("link").asText();
        String token = link.substring(link.indexOf("token=") + 6);
        post(null, "/api/auth/register-teacher?token=" + token, Map.of("name", "Wrong", "email", uniqueEmail("other"), "password", PASSWORD, "role", "TEACHER")).andExpect(status().isBadRequest());
        post(null, "/api/auth/register-teacher?token=" + token, Map.of("name", "Invited", "email", email, "password", PASSWORD, "role", "STUDENT")).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("TEACHER"));
        post(null, "/api/auth/register-teacher?token=" + token, Map.of("name", "Again", "email", email, "password", PASSWORD, "role", "TEACHER")).andExpect(status().isBadRequest());
    }

    @Test
    void studentsCannotCreateTeacherInvitations() throws Exception {
        post(student(), "/api/teacher/invitations", Map.of("email", uniqueEmail("x"))).andExpect(status().isForbidden());
    }

    @Test
    void emailNotificationPreferenceDefaultsOnAndCanBeTurnedOff() throws Exception {
        Sess s = student();
        assertThat(s.user().get("emailNotifications").asBoolean()).isTrue();
        put(s, "/api/auth/profile", Map.of("name", "Renamed", "emailNotifications", false))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailNotifications").value(false));
        // omitting the flag leaves it unchanged
        put(s, "/api/auth/profile", Map.of("name", "Renamed Again"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailNotifications").value(false));
    }
}
