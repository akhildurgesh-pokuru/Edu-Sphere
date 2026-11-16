package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommunityAndAssignmentIntegrationTest extends IntegrationTestBase {

    private Map<String, Object> assignment(String title, Instant due) {
        return Map.of("title", title, "description", "d", "instructions", "i", "dueAt", due.toString(), "maxMarks", 100, "allowedTypes", java.util.List.of("pdf", "txt"));
    }

    @Test
    void assignmentDeadlineAndFileTypesAreEnforcedByTheBackend() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        long open = idOf(post(t, "/api/teacher/subjects/" + ids[0] + "/assignments", assignment("Open", Instant.now().plus(2, ChronoUnit.DAYS))).andExpect(status().isCreated()));
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'NEW_ASSIGNMENT'", Integer.class, s.id())).isEqualTo(1);

        upload(s, "/api/student/assignments/" + open + "/submit", "work.docx", "application/octet-stream", new byte[]{'P', 'K', 3, 4}).andExpect(status().isBadRequest()); // docx not allowed here
        upload(s, "/api/student/assignments/" + open + "/submit", "work.pdf", "application/pdf", "not a pdf".getBytes()).andExpect(status().isBadRequest());
        upload(s, "/api/student/assignments/" + open + "/submit", "work.pdf", "application/pdf", PDF).andExpect(status().isOk()).andExpect(jsonPath("$.file.name").value("work.pdf"));
        upload(s, "/api/student/assignments/" + open + "/submit", "work2.pdf", "application/pdf", PDF).andExpect(status().isOk()); // resubmit until due
        assertThat(jdbc.queryForObject("select count(*) from assignment_submissions where assignment_id = ?", Integer.class, open)).isEqualTo(1);

        jdbc.update("update assignments set due_at = date_sub(utc_timestamp(), interval 1 hour) where id = ?", open);
        upload(s, "/api/student/assignments/" + open + "/submit", "late.pdf", "application/pdf", PDF).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("The deadline for this assignment has passed"));

        // outsiders and teachers cannot submit
        upload(student(), "/api/student/assignments/" + open + "/submit", "x.pdf", "application/pdf", PDF).andExpect(status().isForbidden());
        upload(t, "/api/student/assignments/" + open + "/submit", "x.pdf", "application/pdf", PDF).andExpect(status().isForbidden());
    }

    @Test
    void teacherGradesSubmissionsAndSeesMissingWork() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess a = student();
        Sess b = student();
        enroll(a, ids[0]);
        enroll(b, ids[0]);
        long as = idOf(post(t, "/api/teacher/subjects/" + ids[0] + "/assignments", assignment("HW", Instant.now().plus(1, ChronoUnit.DAYS))).andExpect(status().isCreated()));
        upload(a, "/api/student/assignments/" + as + "/submit", "work.pdf", "application/pdf", PDF).andExpect(status().isOk());
        long subId = jdbc.queryForObject("select id from assignment_submissions where assignment_id = ?", Long.class, as);
        long fileId = jdbc.queryForObject("select resource_id from assignment_submissions where id = ?", Long.class, subId);

        JsonNode rows = body(get(t, "/api/teacher/assignments/" + as + "/submissions"));
        assertThat(rows).hasSize(2);
        jdbc.update("update assignments set due_at = date_sub(utc_timestamp(), interval 1 hour) where id = ?", as);
        rows = body(get(t, "/api/teacher/assignments/" + as + "/submissions"));
        long missing = 0, submitted = 0;
        for (JsonNode r : rows) { if ("MISSING".equals(r.get("status").asText())) missing++; if ("SUBMITTED".equals(r.get("status").asText())) submitted++; }
        assertThat(missing).isEqualTo(1);
        assertThat(submitted).isEqualTo(1);

        post(t, "/api/teacher/submissions/" + subId + "/grade", Map.of("marks", 150, "feedback", "too high")).andExpect(status().isBadRequest());
        post(t, "/api/teacher/submissions/" + subId + "/grade", Map.of("marks", 88, "feedback", "Nice")).andExpect(status().isOk()).andExpect(jsonPath("$.marks").value(88.0));
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'ASSIGNMENT_GRADED'", Integer.class, a.id())).isEqualTo(1);

        // file access: only the submitting student and the owner teacher
        get(a, "/api/resources/" + fileId).andExpect(status().isOk());
        get(b, "/api/resources/" + fileId).andExpect(status().isNotFound());
        get(t, "/api/resources/" + fileId).andExpect(status().isOk());
        get(teacher(), "/api/resources/" + fileId).andExpect(status().isNotFound());
        // another teacher cannot grade
        post(teacher(), "/api/teacher/submissions/" + subId + "/grade", Map.of("marks", 1)).andExpect(status().isNotFound());
        get(teacher(), "/api/teacher/assignments/" + as + "/submissions").andExpect(status().isNotFound());
    }

    @Test
    void discussionsAreLimitedToOwnerAndEnrolledStudents() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        Sess outsider = student();
        enroll(s, ids[0]);
        post(outsider, "/api/subjects/" + ids[0] + "/discussions", Map.of("title", "Hi", "body", "there")).andExpect(status().isForbidden());
        post(teacher(), "/api/subjects/" + ids[0] + "/discussions", Map.of("title", "Hi", "body", "there")).andExpect(status().isNotFound());

        String xss = "<script>alert('x')</script>";
        long th = idOf(post(s, "/api/subjects/" + ids[0] + "/discussions", Map.of("title", "Question", "body", xss)).andExpect(status().isCreated()));
        // user text is stored verbatim (React renders it escaped) and returned as JSON string data
        get(s, "/api/discussions/" + th).andExpect(status().isOk()).andExpect(jsonPath("$.body").value(xss));
        get(outsider, "/api/discussions/" + th).andExpect(status().isForbidden());
        post(outsider, "/api/discussions/" + th + "/replies", Map.of("body", "spam")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'DISCUSSION_REPLY'", Integer.class, t.id())).isEqualTo(1);

        post(t, "/api/discussions/" + th + "/replies", Map.of("body", "Answer")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'DISCUSSION_REPLY'", Integer.class, s.id())).isEqualTo(1);

        // unread indicator: teacher posted after student's last read
        JsonNode list = body(get(s, "/api/subjects/" + ids[0] + "/discussions"));
        assertThat(list.get("content").get(0).get("unread").asBoolean()).isTrue();
        get(s, "/api/discussions/" + th).andExpect(status().isOk());
        list = body(get(s, "/api/subjects/" + ids[0] + "/discussions"));
        assertThat(list.get("content").get(0).get("unread").asBoolean()).isFalse();
        assertThat(list.get("content").get(0).get("replyCount").asLong()).isEqualTo(1);

        // search, pin and close are teacher-only moderation
        assertThat(body(get(s, "/api/subjects/" + ids[0] + "/discussions?q=nomatch")).get("content")).isEmpty();
        post(s, "/api/teacher/discussions/" + th + "/pin", Map.of("value", true)).andExpect(status().isForbidden());
        post(teacher(), "/api/teacher/discussions/" + th + "/pin", Map.of("value", true)).andExpect(status().isNotFound());
        post(t, "/api/teacher/discussions/" + th + "/pin", Map.of("value", true)).andExpect(status().isOk());
        post(t, "/api/teacher/discussions/" + th + "/close", Map.of("value", true)).andExpect(status().isOk());
        post(s, "/api/discussions/" + th + "/replies", Map.of("body", "more")).andExpect(status().isConflict());
        post(t, "/api/discussions/" + th + "/replies", Map.of("body", "teacher can still reply")).andExpect(status().isCreated());

        // delete own post only
        long other = idOf(post(t, "/api/subjects/" + ids[0] + "/discussions", Map.of("title", "T", "body", "b")).andExpect(status().isCreated()));
        delete(s, "/api/discussions/" + other).andExpect(status().isForbidden());
        delete(s, "/api/discussions/" + th).andExpect(status().isOk());
    }

    @Test
    void announcementsAreTeacherOnlyAndNotifyStudents() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        post(s, "/api/teacher/subjects/" + ids[0] + "/announcements", Map.of("title", "x", "body", "y")).andExpect(status().isForbidden());
        post(teacher(), "/api/teacher/subjects/" + ids[0] + "/announcements", Map.of("title", "x", "body", "y")).andExpect(status().isNotFound());
        long an = idOf(post(t, "/api/teacher/subjects/" + ids[0] + "/announcements", Map.of("title", "Exam moved", "body", "Friday", "pinned", true)).andExpect(status().isCreated()));
        get(s, "/api/subjects/" + ids[0] + "/announcements").andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Exam moved"));
        get(s, "/api/student/announcements").andExpect(status().isOk()).andExpect(jsonPath("$[0].subjectTitle").exists());
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'ANNOUNCEMENT'", Integer.class, s.id())).isEqualTo(1);
        put(s, "/api/teacher/announcements/" + an, Map.of("title", "x", "body", "y")).andExpect(status().isForbidden());
        put(t, "/api/teacher/announcements/" + an, Map.of("title", "Exam moved again", "body", "Saturday")).andExpect(status().isOk());
        delete(t, "/api/teacher/announcements/" + an).andExpect(status().isOk());
        get(outsiderOf(), "/api/subjects/" + ids[0] + "/announcements").andExpect(status().isForbidden());
    }

    private Sess outsiderOf() throws Exception { return student(); }

    @Test
    void notificationsBelongToTheirOwner() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        long myNotif = jdbc.queryForObject("select id from notifications where user_id = ? limit 1", Long.class, s.id());
        assertThat(body(get(s, "/api/notifications/unread-count")).get("count").asLong()).isGreaterThanOrEqualTo(1);
        post(t, "/api/notifications/" + myNotif + "/read", null).andExpect(status().isNotFound()); // someone else's
        post(s, "/api/notifications/" + myNotif + "/read", null).andExpect(status().isOk());
        post(s, "/api/notifications/read-all", null).andExpect(status().isOk());
        assertThat(body(get(s, "/api/notifications/unread-count")).get("count").asLong()).isZero();
        get(s, "/api/notifications?page=0&size=500").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50)); // size is capped
    }

    @Test
    void dashboardsAndAnalyticsComeFromStoredActivityAndRespectOwnership() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        post(s, "/api/student/lessons/" + ids[2] + "/complete", null).andExpect(status().isOk());
        JsonNode dash = body(get(t, "/api/teacher/dashboard").andExpect(status().isOk()));
        assertThat(dash.get("totals").get("subjects").asInt()).isEqualTo(1);
        assertThat(dash.get("totals").get("enrolledStudents").asInt()).isEqualTo(1);
        assertThat(dash.get("totals").get("lessons").asInt()).isEqualTo(1);
        assertThat(dash.get("enrollmentsOverTime")).hasSize(30);
        JsonNode an = body(get(t, "/api/teacher/subjects/" + ids[0] + "/analytics").andExpect(status().isOk()));
        assertThat(an.get("averageProgress").asDouble()).isEqualTo(100.0);
        assertThat(an.get("completionRate").asDouble()).isEqualTo(100.0);
        get(teacher(), "/api/teacher/subjects/" + ids[0] + "/analytics").andExpect(status().isNotFound());
        JsonNode sd = body(get(s, "/api/student/dashboard").andExpect(status().isOk()));
        assertThat(sd.get("overallPercent").asDouble()).isEqualTo(100.0);
        assertThat(sd.get("continueLearning").get("subjectId").asLong()).isEqualTo(ids[0]);
        get(s, "/api/student/progress").andExpect(status().isOk()).andExpect(jsonPath("$.subjects[0].percent").value(100.0));
        get(s, "/api/teacher/dashboard").andExpect(status().isForbidden());
    }

    @Test
    void searchIsRoleAwareAndParameterized() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        get(t, "/api/search?q=Java").andExpect(status().isOk()).andExpect(jsonPath("$.subjects[0].link").exists());
        JsonNode students = body(get(t, "/api/search?q=" + s.user().get("email").asText().substring(0, 12))).get("students");
        assertThat(students).isNotEmpty();
        get(s, "/api/search?q=Java").andExpect(status().isOk()).andExpect(jsonPath("$.students").isEmpty());
        get(s, "/api/search?q=' OR 1=1 --").andExpect(status().isOk()).andExpect(jsonPath("$.subjects").isEmpty());
        get(teacher(), "/api/search?q=Java").andExpect(jsonPath("$.subjects").isEmpty()); // another teacher: nothing of mine
    }
}
