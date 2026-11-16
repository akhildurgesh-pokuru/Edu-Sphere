package com.edusphere.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuizIntegrationTest extends IntegrationTestBase {

    private long quiz(Sess t, long subjectId, int attempts, boolean showResults) throws Exception {
        return idOf(post(t, "/api/teacher/subjects/" + subjectId + "/quizzes", Map.of("title", "Quiz", "durationMinutes", 10, "maxAttempts", attempts,
                "passingPercentage", 50, "showResults", showResults)).andExpect(status().isCreated()));
    }

    private void addQuestions(Sess t, long quizId) throws Exception {
        post(t, "/api/teacher/quizzes/" + quizId + "/questions", Map.of("type", "SINGLE_CHOICE", "text", "JVM stands for?", "marks", 2.0, "explanation", "Because.",
                "options", List.of(Map.of("text", "Java Virtual Machine", "correct", true), Map.of("text", "Java Very Much", "correct", false)))).andExpect(status().isCreated());
        post(t, "/api/teacher/quizzes/" + quizId + "/questions", Map.of("type", "MULTIPLE_SELECT", "text", "Pick primes", "marks", 2.0,
                "options", List.of(Map.of("text", "2", "correct", true), Map.of("text", "3", "correct", true), Map.of("text", "4", "correct", false)))).andExpect(status().isCreated());
        post(t, "/api/teacher/quizzes/" + quizId + "/questions", Map.of("type", "TRUE_FALSE", "text", "Java is compiled to bytecode", "marks", 1.0, "trueFalseAnswer", true)).andExpect(status().isCreated());
        post(t, "/api/teacher/quizzes/" + quizId + "/questions", Map.of("type", "SHORT_ANSWER", "text", "Name an OOP principle", "marks", 3.0)).andExpect(status().isCreated());
    }

    /** Builds answers choosing correct options (as seen by the teacher) for objective questions. */
    private Map<String, Object> answersFor(JsonNode attemptView, JsonNode teacherQuiz, boolean allCorrect, String shortText) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode q : attemptView.get("questions")) {
            long qid = q.get("id").asLong();
            JsonNode tq = null;
            for (JsonNode x : teacherQuiz.get("questions")) if (x.get("id").asLong() == qid) tq = x;
            if ("SHORT_ANSWER".equals(q.get("type").asText())) { out.add(Map.of("questionId", qid, "text", shortText)); continue; }
            List<Long> chosen = new ArrayList<>();
            for (JsonNode o : tq.get("options")) if (o.get("correct").asBoolean() == allCorrect) chosen.add(o.get("id").asLong());
            if (!allCorrect && "SINGLE_CHOICE".equals(q.get("type").asText()) || "TRUE_FALSE".equals(q.get("type").asText()) && !allCorrect) chosen = chosen.subList(0, 1);
            out.add(Map.of("questionId", qid, "optionIds", chosen));
        }
        return Map.of("answers", out);
    }

    @Test
    void fullQuizFlowWithAutoAndManualGrading() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 2, true);
        addQuestions(t, qz);
        Sess s = student();
        enroll(s, ids[0]);
        // not published yet
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isNotFound());
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'NEW_QUIZ'", Integer.class, s.id())).isEqualTo(1);

        String startJson = post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(startJson).doesNotContain("\"correct\"").doesNotContain("explanation");   // correct answers never sent early
        JsonNode view = json.readTree(startJson);
        long attemptId = view.get("attemptId").asLong();
        assertThat(view.get("questions")).hasSize(4);
        assertThat(java.time.Instant.parse(view.get("deadlineAt").asText())).isAfter(java.time.Instant.now().plusSeconds(500)).isBefore(java.time.Instant.now().plusSeconds(620)); // server-computed 10 min

        // resuming returns the same attempt
        assertThat(post(s, "/api/student/quizzes/" + qz + "/start", null).andReturn().getResponse().getContentAsString()).contains("\"attemptId\":" + attemptId);

        JsonNode teacherQuiz = body(get(t, "/api/teacher/quizzes/" + qz));
        post(s, "/api/student/attempts/" + attemptId + "/answers", null);
        put(s, "/api/student/attempts/" + attemptId + "/answers", answersFor(view, teacherQuiz, true, "Encapsulation")).andExpect(status().isOk());
        get(s, "/api/student/attempts/" + attemptId).andExpect(status().isOk()).andExpect(jsonPath("$.questions").isArray()); // still in progress

        JsonNode result = body(post(s, "/api/student/attempts/" + attemptId + "/submit", answersFor(view, teacherQuiz, true, "Encapsulation")).andExpect(status().isOk()));
        assertThat(result.get("score").asDouble()).isEqualTo(5.0);          // 2 + 2 + 1 auto-graded
        assertThat(result.get("maxScore").asDouble()).isEqualTo(8.0);
        assertThat(result.get("status").asText()).isEqualTo("PENDING_GRADING");
        assertThat(result.get("reviewAllowed").asBoolean()).isTrue();

        // duplicate submission and late saves are rejected
        post(s, "/api/student/attempts/" + attemptId + "/submit", answersFor(view, teacherQuiz, true, "x")).andExpect(status().isConflict());
        put(s, "/api/student/attempts/" + attemptId + "/answers", answersFor(view, teacherQuiz, false, "x")).andExpect(status().isConflict());

        // teacher grades the short answer
        JsonNode review = body(get(t, "/api/teacher/attempts/" + attemptId));
        long shortAnswerId = 0;
        for (JsonNode r : review.get("review")) if ("SHORT_ANSWER".equals(r.get("type").asText())) shortAnswerId = r.get("answerId").asLong();
        post(t, "/api/teacher/attempts/" + attemptId + "/grade", Map.of("grades", List.of(Map.of("answerId", shortAnswerId, "marks", 99.0)))).andExpect(status().isBadRequest()); // above max
        JsonNode graded = body(post(t, "/api/teacher/attempts/" + attemptId + "/grade", Map.of("grades", List.of(Map.of("answerId", shortAnswerId, "marks", 2.5, "feedback", "Good")))).andExpect(status().isOk()));
        assertThat(graded.get("status").asText()).isEqualTo("GRADED");
        assertThat(graded.get("score").asDouble()).isEqualTo(7.5);
        assertThat(jdbc.queryForObject("select count(*) from quiz_grade_history where answer_id = ?", Integer.class, shortAnswerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = 'QUIZ_RESULT'", Integer.class, s.id())).isEqualTo(1);
        // regrading keeps history
        post(t, "/api/teacher/attempts/" + attemptId + "/grade", Map.of("grades", List.of(Map.of("answerId", shortAnswerId, "marks", 3.0)))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from quiz_grade_history where answer_id = ?", Integer.class, shortAnswerId)).isEqualTo(2);

        // student now sees the review with correct answers
        JsonNode mine = body(get(s, "/api/student/attempts/" + attemptId).andExpect(status().isOk()));
        assertThat(mine.get("review").get(0).get("correctOptionIds")).isNotEmpty();

        // second attempt allowed, third not
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isOk());
    }

    @Test
    void attemptLimitIsEnforcedOnTheServer() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 1, true);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        JsonNode view = body(post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isOk()));
        post(s, "/api/student/attempts/" + view.get("attemptId").asLong() + "/submit", Map.of("answers", List.of())).andExpect(status().isOk());
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isConflict());
    }

    @Test
    void expiredAttemptsAreClosedWithSavedAnswersOnly() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 2, true);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        JsonNode view = body(post(s, "/api/student/quizzes/" + qz + "/start", null));
        long attemptId = view.get("attemptId").asLong();
        JsonNode teacherQuiz = body(get(t, "/api/teacher/quizzes/" + qz));
        // save only the first (correct) answer while time remains
        JsonNode first = view.get("questions").get(0);
        long correctOpt = 0;
        for (JsonNode x : teacherQuiz.get("questions")) if (x.get("id").asLong() == first.get("id").asLong()) for (JsonNode o : x.get("options")) if (o.get("correct").asBoolean()) correctOpt = o.get("id").asLong();
        put(s, "/api/student/attempts/" + attemptId + "/answers", Map.of("answers", List.of(Map.of("questionId", first.get("id").asLong(), "optionIds", List.of(correctOpt))))).andExpect(status().isOk());
        // time runs out
        jdbc.update("update quiz_attempts set deadline_at = date_sub(utc_timestamp(), interval 2 minute) where id = ?", attemptId);
        // a late submit cannot add answers
        JsonNode result = body(post(s, "/api/student/attempts/" + attemptId + "/submit", answersFor(view, teacherQuiz, true, "late")).andExpect(status().isOk()));
        assertThat(result.get("expired").asBoolean()).isTrue();
        assertThat(result.get("score").asDouble()).isLessThan(5.0);   // late answers were ignored
        assertThat(result.get("score").asDouble()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void stalledAttemptsAreAutoSubmittedWhenListed() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 2, false);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        long attemptId = body(post(s, "/api/student/quizzes/" + qz + "/start", null)).get("attemptId").asLong();
        jdbc.update("update quiz_attempts set deadline_at = date_sub(utc_timestamp(), interval 1 hour) where id = ?", attemptId);
        get(s, "/api/student/subjects/" + ids[0] + "/quizzes").andExpect(status().isOk()).andExpect(jsonPath("$[0].inProgressAttemptId").doesNotExist());
        assertThat(jdbc.queryForObject("select status from quiz_attempts where id = ?", String.class, attemptId)).isIn("GRADED", "PENDING_GRADING");
        // showResults=false hides the review
        get(s, "/api/student/attempts/" + attemptId).andExpect(jsonPath("$.reviewAllowed").value(false)).andExpect(jsonPath("$.review").isEmpty());
    }

    @Test
    void studentsCannotReachOthersAttemptsOrUnenrolledQuizzes() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 2, true);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess a = student();
        enroll(a, ids[0]);
        long attemptId = body(post(a, "/api/student/quizzes/" + qz + "/start", null)).get("attemptId").asLong();
        Sess outsider = student();
        post(outsider, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isForbidden());
        get(outsider, "/api/student/attempts/" + attemptId).andExpect(status().isNotFound());
        put(outsider, "/api/student/attempts/" + attemptId + "/answers", Map.of("answers", List.of())).andExpect(status().isNotFound());
        post(outsider, "/api/student/attempts/" + attemptId + "/submit", Map.of("answers", List.of())).andExpect(status().isNotFound());
        Sess enrolledOther = student();
        enroll(enrolledOther, ids[0]);
        get(enrolledOther, "/api/student/attempts/" + attemptId).andExpect(status().isNotFound());
        // teachers: only the owner may see attempts
        get(teacher(), "/api/teacher/attempts/" + attemptId).andExpect(status().isNotFound());
        get(teacher(), "/api/teacher/quizzes/" + qz).andExpect(status().isNotFound());
        get(teacher(), "/api/teacher/quizzes/" + qz + "/attempts").andExpect(status().isNotFound());
        // students never see the teacher view with correct answers
        get(a, "/api/teacher/quizzes/" + qz).andExpect(status().isForbidden());
    }

    @Test
    void questionsAreValidatedAndLockedAfterAttempts() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 1, true);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isBadRequest()); // no questions
        post(t, "/api/teacher/quizzes/" + qz + "/questions", Map.of("type", "SINGLE_CHOICE", "text", "Q", "marks", 1.0,
                "options", List.of(Map.of("text", "A", "correct", true), Map.of("text", "B", "correct", true)))).andExpect(status().isBadRequest()); // two correct on single choice
        post(t, "/api/teacher/quizzes/" + qz + "/questions", Map.of("type", "SINGLE_CHOICE", "text", "Q", "marks", 1.0, "options", List.of(Map.of("text", "A", "correct", true)))).andExpect(status().isBadRequest());
        post(t, "/api/teacher/quizzes/" + qz + "/questions", Map.of("type", "NOPE", "text", "Q", "marks", 1.0)).andExpect(status().isBadRequest());
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isOk());
        post(t, "/api/teacher/quizzes/" + qz + "/questions", Map.of("type", "SHORT_ANSWER", "text", "late", "marks", 1.0)).andExpect(status().isConflict());
    }

    @Test
    void closedAndFutureQuizzesCannotBeStarted() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 3, true);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        jdbc.update("update quizzes set end_at = date_sub(utc_timestamp(), interval 1 day) where id = ?", qz);
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isConflict());
        jdbc.update("update quizzes set end_at = null, start_at = date_add(utc_timestamp(), interval 1 day) where id = ?", qz);
        post(s, "/api/student/quizzes/" + qz + "/start", null).andExpect(status().isConflict());
    }

    @Test
    void csvExportNeutralisesFormulaInjection() throws Exception {
        Sess t = teacher();
        long[] ids = publishedSubject(t);
        long qz = quiz(t, ids[0], 1, true);
        addQuestions(t, qz);
        post(t, "/api/teacher/quizzes/" + qz + "/publish", Map.of("published", true)).andExpect(status().isOk());
        Sess s = student();
        enroll(s, ids[0]);
        jdbc.update("update users set name = '=HYPERLINK(\"http://evil\")' where id = ?", s.id());
        long attemptId = body(post(s, "/api/student/quizzes/" + qz + "/start", null)).get("attemptId").asLong();
        post(s, "/api/student/attempts/" + attemptId + "/submit", Map.of("answers", List.of())).andExpect(status().isOk());
        String csv = get(t, "/api/teacher/quizzes/" + qz + "/export.csv").andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv")).andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("Student,Email").contains("\"'=HYPERLINK(").doesNotContain(",\"=HYPERLINK");
        get(teacher(), "/api/teacher/quizzes/" + qz + "/export.csv").andExpect(status().isNotFound());
    }
}
