package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SubjectAccessIntegrationTest extends IntegrationTestBase {

    @Test
    void studentCannotUseTeacherApisAndTeacherCannotUseStudentApis() throws Exception {
        get(student(), "/api/teacher/subjects").andExpect(status().isForbidden());
        post(student(), "/api/teacher/subjects", Map.of("title", "Hack")).andExpect(status().isForbidden());
        get(teacher(), "/api/student/catalog").andExpect(status().isForbidden());
        post(teacher(), "/api/student/subjects/1/enroll", null).andExpect(status().isForbidden());
    }

    @Test
    void draftSubjectsAreInvisibleToStudents() throws Exception {
        Sess t = teacher();
        long id = createSubject(t, "Secret Draft " + System.nanoTime());
        Sess s = student();
        get(s, "/api/student/subjects/" + id).andExpect(status().isNotFound());
        post(s, "/api/student/subjects/" + id + "/enroll", null).andExpect(status().isNotFound());
        assertThat(body(get(s, "/api/student/catalog?q=Secret Draft")).get("content")).isEmpty();
    }

    @Test
    void publishRequiresAPublishedLesson() throws Exception {
        Sess t = teacher();
        long id = createSubject(t, "Empty");
        post(t, "/api/teacher/subjects/" + id + "/status", Map.of("status", "PUBLISHED")).andExpect(status().isBadRequest());
        long m = createModule(t, id);
        createTextLesson(t, m, false);
        post(t, "/api/teacher/subjects/" + id + "/status", Map.of("status", "PUBLISHED")).andExpect(status().isBadRequest());
        createTextLesson(t, m, true);
        post(t, "/api/teacher/subjects/" + id + "/status", Map.of("status", "PUBLISHED")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void teachersNeverSeeEachOthersSubjects() throws Exception {
        Sess a = teacher();
        Sess b = teacher();
        long id = createSubject(a, "Teacher A course");
        get(b, "/api/teacher/subjects/" + id).andExpect(status().isNotFound());
        put(b, "/api/teacher/subjects/" + id, Map.of("title", "Stolen")).andExpect(status().isNotFound());
        post(b, "/api/teacher/subjects/" + id + "/status", Map.of("status", "PUBLISHED")).andExpect(status().isNotFound());
        get(b, "/api/teacher/subjects/" + id + "/curriculum").andExpect(status().isNotFound());
        get(b, "/api/teacher/subjects/" + id + "/students").andExpect(status().isNotFound());
        post(b, "/api/teacher/subjects/" + id + "/modules", Map.of("title", "x")).andExpect(status().isNotFound());
        assertThat(body(get(b, "/api/teacher/subjects"))).isEmpty();
    }

    @Test
    void enrollmentIsOneClickAndDuplicatesAreRejected() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        post(s, "/api/student/subjects/" + ids[0] + "/enroll", null).andExpect(status().isOk()).andExpect(jsonPath("$.enrolled").value(true));
        post(s, "/api/student/subjects/" + ids[0] + "/enroll", null).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from enrollments where subject_id = ?", Integer.class, ids[0])).isEqualTo(1);
        assertThat(body(get(s, "/api/student/subjects"))).hasSize(1);
        assertThat(body(get(t, "/api/teacher/subjects/" + ids[0] + "/students"))).hasSize(1);
    }

    @Test
    void lessonsAreLockedUntilEnrolledEvenWhenEditingIds() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        get(s, "/api/lessons/" + ids[2]).andExpect(status().isForbidden());
        get(s, "/api/subjects/" + ids[0] + "/discussions").andExpect(status().isForbidden());
        get(s, "/api/subjects/" + ids[0] + "/announcements").andExpect(status().isForbidden());
        get(s, "/api/student/subjects/" + ids[0] + "/quizzes").andExpect(status().isForbidden());
        get(s, "/api/student/subjects/" + ids[0] + "/assignments").andExpect(status().isForbidden());
        get(s, "/api/student/subjects/" + ids[0] + "/progress").andExpect(status().isForbidden());
        put(s, "/api/student/lessons/" + ids[2] + "/position", Map.of("position", 10)).andExpect(status().isForbidden());
        // the outline is visible but flagged locked
        get(s, "/api/student/subjects/" + ids[0] + "/curriculum").andExpect(status().isOk()).andExpect(jsonPath("$.locked").value(true));
        enroll(s, ids[0]);
        get(s, "/api/lessons/" + ids[2]).andExpect(status().isOk()).andExpect(jsonPath("$.contentText").value("Hello"));
        get(s, "/api/student/subjects/" + ids[0] + "/curriculum").andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    void draftLessonsStayHiddenFromEnrolledStudents() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long draft = createTextLesson(t, ids[1], false);
        Sess s = student();
        enroll(s, ids[0]);
        get(s, "/api/lessons/" + draft).andExpect(status().isNotFound());
        assertThat(body(get(s, "/api/student/subjects/" + ids[0] + "/curriculum")).get("modules").get(0).get("lessons")).hasSize(1);
        get(t, "/api/lessons/" + draft).andExpect(status().isOk());
    }

    @Test
    void anotherTeacherCannotReadLessonsOfASubjectTheyDoNotOwn() throws Exception {
        Sess a = teacher();
        long[] ids = publishedSubject(a);
        get(teacher(), "/api/lessons/" + ids[2]).andExpect(status().isNotFound());
    }

    @Test
    void leavingAndRemovingStudents() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        post(s, "/api/student/subjects/" + ids[0] + "/leave", null).andExpect(status().isOk());
        get(s, "/api/lessons/" + ids[2]).andExpect(status().isForbidden());
        enroll(s, ids[0]); // LEFT students may come back
        delete(t, "/api/teacher/subjects/" + ids[0] + "/students/" + s.id()).andExpect(status().isOk());
        post(s, "/api/student/subjects/" + ids[0] + "/enroll", null).andExpect(status().isForbidden()); // REMOVED blocks re-enrolling
        get(s, "/api/lessons/" + ids[2]).andExpect(status().isForbidden());
    }

    @Test
    void catalogSearchIsParameterizedAndPaged() throws Exception {
        Sess t = teacher();
        publishedSubject(t);
        Sess s = student();
        get(s, "/api/student/catalog?q=' OR '1'='1").andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        get(s, "/api/student/catalog?q=%25").andExpect(status().isOk());
        get(s, "/api/student/catalog?page=0").andExpect(status().isOk()).andExpect(jsonPath("$.size").value(12));
        get(s, "/api/student/catalog?category=Programming").andExpect(status().isOk());
        get(s, "/api/student/catalog?page=abc").andExpect(status().isBadRequest());
    }

    @Test
    void progressIsServerSideAndClamped() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long second = createTextLesson(t, ids[1], true);
        Sess s = student();
        enroll(s, ids[0]);
        get(s, "/api/student/subjects/" + ids[0] + "/progress").andExpect(jsonPath("$.percent").value(0.0)).andExpect(jsonPath("$.totalLessons").value(2));
        post(s, "/api/student/lessons/" + ids[2] + "/complete", null).andExpect(status().isOk());
        get(s, "/api/student/subjects/" + ids[0] + "/progress").andExpect(jsonPath("$.percent").value(50.0)).andExpect(jsonPath("$.completedLessons").value(1));
        // absurd positions are clamped, negative becomes zero
        put(s, "/api/student/lessons/" + second + "/position", Map.of("position", 99999999)).andExpect(status().isOk()).andExpect(jsonPath("$.lastPositionSeconds").value(43200));
        put(s, "/api/student/lessons/" + second + "/position", Map.of("position", -50)).andExpect(status().isOk()).andExpect(jsonPath("$.lastPositionSeconds").value(0));
        post(s, "/api/student/lessons/" + second + "/complete", null).andExpect(status().isOk());
        get(s, "/api/student/subjects/" + ids[0] + "/progress").andExpect(jsonPath("$.percent").value(100.0));
        delete(s, "/api/student/lessons/" + ids[2] + "/complete").andExpect(status().isOk());
        get(s, "/api/student/subjects/" + ids[0] + "/progress").andExpect(jsonPath("$.percent").value(50.0));
    }

    @Test
    void curriculumReorderRequiresTheExactIdSet() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long m2 = createModule(t, ids[0]);
        post(t, "/api/teacher/subjects/" + ids[0] + "/modules/reorder", Map.of("ids", java.util.List.of(m2, ids[1]))).andExpect(status().isOk());
        assertThat(body(get(t, "/api/teacher/subjects/" + ids[0] + "/curriculum")).get("modules").get(0).get("id").asLong()).isEqualTo(m2);
        post(t, "/api/teacher/subjects/" + ids[0] + "/modules/reorder", Map.of("ids", java.util.List.of(m2))).andExpect(status().isBadRequest());
        post(teacher(), "/api/teacher/subjects/" + ids[0] + "/modules/reorder", Map.of("ids", java.util.List.of(m2, ids[1]))).andExpect(status().isNotFound());
    }
}
