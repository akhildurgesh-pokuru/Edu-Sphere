package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationIntegrationTest extends IntegrationTestBase {

    @Test
    void flywayCreatedEveryTableAndHibernateValidatedTheSchema() {
        List<String> tables = jdbc.queryForList("select table_name from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE'", String.class)
                .stream().map(String::toLowerCase).toList();
        assertThat(tables).contains("users", "roles", "user_roles", "oauth_accounts", "teacher_invitations", "password_reset_tokens",
                "subjects", "modules", "lessons", "learning_resources", "enrollments", "enrollment_invitations", "enrollment_requests",
                "subject_join_codes", "lesson_progress", "quizzes", "quiz_questions", "quiz_options", "quiz_attempts", "quiz_answers",
                "quiz_grade_history", "assignments", "assignment_submissions", "discussions", "discussion_replies", "discussion_reads",
                "announcements", "notifications", "flyway_schema_history");
    }

    @Test
    void rolesAreSeededAndConstraintsExist() {
        assertThat(jdbc.queryForList("select name from roles order by name", String.class)).containsExactly("STUDENT", "TEACHER");
        String[][] uniques = {{"enrollments", "uq_enrollments_subject_id_student_id"}, {"quiz_attempts", "uq_quiz_attempts_quiz_id_student_id_attempt_no"},
                {"assignment_submissions", "uq_assignment_submissions_assignment_id_student_id"}, {"lesson_progress", "uq_lesson_progress_lesson_id_student_id"}};
        for (String[] u : uniques) {
            Integer c = jdbc.queryForObject("select count(*) from information_schema.statistics where table_schema = database() and table_name = ? and index_name = ? and non_unique = 0", Integer.class, u[0], u[1]);
            assertThat(c).as(u[1]).isGreaterThan(0);
        }
    }

    @Test
    void databaseItselfBlocksDuplicateEnrollments() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        Sess s = student();
        enroll(s, ids[0]);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> jdbc.update("insert into enrollments (subject_id, student_id, status) values (?, ?, 'ACTIVE')", ids[0], s.id()));
    }
}
