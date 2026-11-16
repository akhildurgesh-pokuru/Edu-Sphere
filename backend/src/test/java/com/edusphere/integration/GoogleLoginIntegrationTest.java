package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@TestPropertySource(properties = {"app.google-enabled=true", "spring.security.oauth2.client.registration.google.client-id=test-client-id"})
class GoogleLoginIntegrationTest extends IntegrationTestBase {

    @Test
    void googleSignInAlwaysAsksWhichAccountToUse() throws Exception {
        MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/oauth2/authorization/google")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(302);
        String location = r.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(location).contains("prompt=select_account").contains("client_id=test-client-id").contains("scope=openid");
    }
}
