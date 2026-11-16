package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lives in its own Spring context (note the extra property): the csrf() test helper used elsewhere swaps the filter's
 * token repository for the whole context, which would hide the real cookie behaviour tested here.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/Edu-Sphere-test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8",
        "spring.flyway.clean-disabled=false", "app.bootstrap-teacher.email=", "app.bootstrap-teacher.password=", "test.context=csrf-cookie"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDbConfig.class)
class CsrfCookieIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void csrfEndpointSetsAScriptReadableLaxCookie() throws Exception {
        var res = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        var cookie = res.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isFalse();      // the SPA must read it to echo it in the header
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void aWriteSucceedsOnlyWhenTheHeaderMatchesTheCookie() throws Exception {
        String token = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
        String body = "{\"email\":\"nobody@test.local\"}";
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content(body)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", "wrong")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content(body)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", token)).header("X-XSRF-TOKEN", token)).andExpect(status().isOk());
    }
}
