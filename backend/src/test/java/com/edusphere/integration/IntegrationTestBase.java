package com.edusphere.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
        // highest-precedence overrides so a developer's local config/application.yml can never point tests at real data
        "spring.datasource.url=jdbc:mysql://localhost:3306/Edu-Sphere-test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8",
        "spring.flyway.clean-disabled=false",
        "app.bootstrap-teacher.email=",
        "app.bootstrap-teacher.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDbConfig.class)
public abstract class IntegrationTestBase {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;

    protected static final String PASSWORD = "Passw0rdX";

    /** A signed-in user: the HTTP session plus the user JSON. */
    protected record Sess(MockHttpSession session, JsonNode user) {
        long id() { return user.get("id").asLong(); }
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    protected Sess register(String role) throws Exception {
        MvcResult r = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + role + " User\",\"email\":\"" + uniqueEmail(role.toLowerCase()) + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"" + role + "\"}"))
                .andReturn();
        if (r.getResponse().getStatus() != 201) throw new IllegalStateException("register failed: " + r.getResponse().getContentAsString());
        return new Sess((MockHttpSession) r.getRequest().getSession(), json.readTree(r.getResponse().getContentAsString()));
    }

    protected Sess teacher() throws Exception { return register("TEACHER"); }
    protected Sess student() throws Exception { return register("STUDENT"); }

    protected ResultActions get(Sess s, String url) throws Exception {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.get(url);
        if (s != null) b.session(s.session());
        return mvc.perform(b);
    }

    protected ResultActions send(MockHttpServletRequestBuilder b, Sess s, Object body) throws Exception {
        b.with(csrf());
        if (s != null) b.session(s.session());
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body instanceof String str ? str : json.writeValueAsString(body));
        return mvc.perform(b);
    }

    protected ResultActions post(Sess s, String url, Object body) throws Exception { return send(MockMvcRequestBuilders.post(url), s, body); }
    protected ResultActions put(Sess s, String url, Object body) throws Exception { return send(MockMvcRequestBuilders.put(url), s, body); }
    protected ResultActions delete(Sess s, String url) throws Exception { return send(MockMvcRequestBuilders.delete(url), s, null); }

    protected ResultActions upload(Sess s, String url, String filename, String contentType, byte[] content) throws Exception {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.multipart(url).file(new MockMultipartFile("file", filename, contentType, content)).with(csrf());
        if (s != null) b.session(s.session());
        return mvc.perform(b);
    }

    protected JsonNode body(ResultActions a) throws Exception {
        return json.readTree(a.andReturn().getResponse().getContentAsString());
    }

    protected long idOf(ResultActions a) throws Exception { return body(a).get("id").asLong(); }

    // ----- fixtures -----
    protected static final byte[] PDF = "%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF".getBytes();

    protected static byte[] mp4() {
        byte[] b = new byte[2048];
        b[3] = 0x18; b[4] = 'f'; b[5] = 't'; b[6] = 'y'; b[7] = 'p';
        return b;
    }

    protected long createSubject(Sess teacher, String title) throws Exception {
        return idOf(post(teacher, "/api/teacher/subjects", java.util.Map.of("title", title, "description", "desc", "category", "Programming")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
    }

    protected long createModule(Sess teacher, long subjectId) throws Exception {
        return idOf(post(teacher, "/api/teacher/subjects/" + subjectId + "/modules", java.util.Map.of("title", "Module 1")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
    }

    protected long createTextLesson(Sess teacher, long moduleId, boolean published) throws Exception {
        return idOf(post(teacher, "/api/teacher/modules/" + moduleId + "/lessons",
                java.util.Map.of("title", "Lesson", "type", "TEXT", "contentText", "Hello", "published", published)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()));
    }

    /** A published subject with one module and one published text lesson. Returns {subjectId, moduleId, lessonId}. */
    protected long[] publishedSubject(Sess teacher) throws Exception {
        long s = createSubject(teacher, "Java " + UUID.randomUUID().toString().substring(0, 6));
        long m = createModule(teacher, s);
        long l = createTextLesson(teacher, m, true);
        post(teacher, "/api/teacher/subjects/" + s + "/status", java.util.Map.of("status", "PUBLISHED")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        return new long[]{s, m, l};
    }

    protected void enroll(Sess student, long subjectId) throws Exception {
        post(student, "/api/student/subjects/" + subjectId + "/enroll", null).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
