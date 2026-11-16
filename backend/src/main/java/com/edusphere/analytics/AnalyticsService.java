package com.edusphere.analytics;

import com.edusphere.common.CurrentUser;
import com.edusphere.subject.AccessService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every number comes from stored activity. Formulas (also in docs/analytics-formulas.md):
 * progress = completed published lessons / published lessons; completion rate = students at 100% / enrolled students;
 * quiz percent = score / max score of GRADED attempts; active = any lesson/quiz/assignment activity in the last 7 days;
 * needs support = quiz average below 50%, or inactive 14+ days below 100% progress, or enrolled 7+ days with no progress.
 */
@Service
public class AnalyticsService {
    private final NamedParameterJdbcTemplate jdbc;
    private final AccessService access;

    public AnalyticsService(NamedParameterJdbcTemplate jdbc, AccessService access) { this.jdbc = jdbc; this.access = access; }

    private static Timestamp ts(Instant i) { return Timestamp.from(i); }
    private static Instant inst(Object o) {
        if (o == null) return null;
        if (o instanceof java.time.LocalDateTime l) return l.toInstant(ZoneOffset.UTC); // DATETIME columns hold UTC wall time
        if (o instanceof Timestamp t) return t.toInstant();
        throw new IllegalStateException("Unexpected date type " + o.getClass());
    }
    private static LocalDate day(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return inst(o).atZone(ZoneOffset.UTC).toLocalDate();
    }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double dbl(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
    private static long lng(Object o) { return o == null ? 0 : ((Number) o).longValue(); }

    private List<Map<String, Object>> q(String sql, MapSqlParameterSource p) { return jdbc.queryForList(sql, p); }

    private Map<Long, Long> publishedLessonTotals(Collection<Long> subjectIds) {
        Map<Long, Long> out = new HashMap<>();
        if (subjectIds.isEmpty()) return out;
        q("select subject_id, count(*) c from lessons where subject_id in (:ids) and published = 1 group by subject_id",
                new MapSqlParameterSource("ids", subjectIds)).forEach(r -> out.put(lng(r.get("subject_id")), lng(r.get("c"))));
        return out;
    }

    /** subject -> student -> completed published lessons, for ACTIVE enrollments. */
    private Map<Long, Map<Long, Long>> doneBySubjectStudent(Collection<Long> subjectIds) {
        Map<Long, Map<Long, Long>> out = new HashMap<>();
        if (subjectIds.isEmpty()) return out;
        q("""
          select e.subject_id, e.student_id, coalesce(sum(case when lp.completed = 1 and l.published = 1 then 1 else 0 end), 0) done
          from enrollments e left join lesson_progress lp on lp.subject_id = e.subject_id and lp.student_id = e.student_id
          left join lessons l on l.id = lp.lesson_id
          where e.status = 'ACTIVE' and e.subject_id in (:ids) group by e.subject_id, e.student_id""",
                new MapSqlParameterSource("ids", subjectIds)).forEach(r ->
                out.computeIfAbsent(lng(r.get("subject_id")), k -> new HashMap<>()).put(lng(r.get("student_id")), lng(r.get("done"))));
        return out;
    }

    /** (subject,student) -> last activity time. */
    private Map<String, Instant> lastActive(Collection<Long> subjectIds) {
        Map<String, Instant> out = new HashMap<>();
        if (subjectIds.isEmpty()) return out;
        q("""
          select subject_id, student_id, max(t) last_at from (
            select lp.subject_id subject_id, lp.student_id student_id, lp.updated_at t from lesson_progress lp where lp.subject_id in (:ids)
            union all select q.subject_id, qa.student_id, qa.started_at from quiz_attempts qa join quizzes q on q.id = qa.quiz_id where q.subject_id in (:ids)
            union all select a.subject_id, sb.student_id, sb.submitted_at from assignment_submissions sb join assignments a on a.id = sb.assignment_id where a.subject_id in (:ids)
          ) x group by subject_id, student_id""", new MapSqlParameterSource("ids", subjectIds))
                .forEach(r -> out.put(r.get("subject_id") + ":" + r.get("student_id"), inst(r.get("last_at"))));
        return out;
    }

    // ---------------- teacher dashboard ----------------
    @Transactional(readOnly = true)
    public Map<String, Object> teacherDashboard() {
        Long t = CurrentUser.id();
        MapSqlParameterSource p = new MapSqlParameterSource("t", t);
        Instant now = Instant.now();
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> sub = q("select count(*) c, coalesce(sum(status = 'PUBLISHED'), 0) p from subjects where teacher_id = :t", p).get(0);
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subjects", lng(sub.get("c")));
        totals.put("publishedSubjects", lng(sub.get("p")));
        totals.put("enrolledStudents", lng(q("select count(distinct e.student_id) c from enrollments e join subjects s on s.id = e.subject_id where s.teacher_id = :t and e.status = 'ACTIVE'", p).get(0).get("c")));
        totals.put("lessons", lng(q("select count(*) c from lessons l join subjects s on s.id = l.subject_id where s.teacher_id = :t", p).get(0).get("c")));
        totals.put("pendingAssignments", lng(q("""
                select count(*) c from assignment_submissions sb join assignments a on a.id = sb.assignment_id join subjects s on s.id = a.subject_id
                where s.teacher_id = :t and sb.marks is null""", p).get(0).get("c")));
        Object avg = q("""
                select avg(qa.score / qa.max_score * 100) a from quiz_attempts qa join quizzes z on z.id = qa.quiz_id join subjects s on s.id = z.subject_id
                where s.teacher_id = :t and qa.status = 'GRADED' and qa.max_score > 0""", p).get(0).get("a");
        totals.put("averageQuizScore", avg == null ? null : round1(dbl(avg)));
        out.put("totals", totals);

        List<Map<String, Object>> subjectRows = q("""
                select s.id, s.title, s.status, count(e.id) c from subjects s left join enrollments e on e.subject_id = s.id and e.status = 'ACTIVE'
                where s.teacher_id = :t group by s.id, s.title, s.status order by c desc, s.title""", p);
        out.put("topCourses", subjectRows.stream().limit(6).map(r -> Map.of("subjectId", r.get("id"), "title", r.get("title"), "enrollments", lng(r.get("c")))).toList());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<LocalDate, Long> perDay = new HashMap<>();
        q("""
          select date(e.enrolled_at) d, count(*) c from enrollments e join subjects s on s.id = e.subject_id
          where s.teacher_id = :t and e.enrolled_at >= :since group by date(e.enrolled_at)""",
                new MapSqlParameterSource("t", t).addValue("since", ts(today.minusDays(29).atStartOfDay().toInstant(ZoneOffset.UTC))))
                .forEach(r -> perDay.put(day(r.get("d")), lng(r.get("c"))));
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            series.add(Map.of("date", d.toString(), "count", perDay.getOrDefault(d, 0L)));
        }
        out.put("enrollmentsOverTime", series);

        List<Long> ids = subjectRows.stream().map(r -> lng(r.get("id"))).toList();
        out.put("engagement", engagement(ids, subjectRows));
        Map<Long, Double> quizAvgBySubject = new HashMap<>();
        if (!ids.isEmpty()) {
            q("""
              select z.subject_id, avg(qa.score / qa.max_score * 100) a from quiz_attempts qa join quizzes z on z.id = qa.quiz_id
              where z.subject_id in (:ids) and qa.status = 'GRADED' and qa.max_score > 0 group by z.subject_id""", new MapSqlParameterSource("ids", ids))
                    .forEach(r -> quizAvgBySubject.put(lng(r.get("subject_id")), round1(dbl(r.get("a")))));
        }
        out.put("studentPerformance", subjectRows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectId", r.get("id"));
            m.put("title", r.get("title"));
            m.put("averageQuizScore", quizAvgBySubject.get(lng(r.get("id"))));
            return m;
        }).toList());

        out.put("recentEnrollments", q("""
                select u.name student, s.title subject, s.id subject_id, e.enrolled_at at from enrollments e join users u on u.id = e.student_id
                join subjects s on s.id = e.subject_id where s.teacher_id = :t and e.status = 'ACTIVE' order by e.enrolled_at desc limit 8""", p)
                .stream().map(r -> Map.of("student", r.get("student"), "subject", r.get("subject"), "subjectId", r.get("subject_id"), "at", inst(r.get("at")))).toList());

        MapSqlParameterSource pn = new MapSqlParameterSource("t", t).addValue("now", ts(now));
        List<Map<String, Object>> deadlines = new ArrayList<>();
        q("""
          select a.id, a.title, s.title subject, s.id subject_id, a.due_at at from assignments a join subjects s on s.id = a.subject_id
          where s.teacher_id = :t and a.due_at > :now order by a.due_at limit 6""", pn).forEach(r -> deadlines.add(Map.of("kind", "ASSIGNMENT", "title", r.get("title"),
                "subject", r.get("subject"), "at", inst(r.get("at")), "link", "/teacher/assignments/" + r.get("id") + "/submissions")));
        q("""
          select z.id, z.title, s.title subject, z.end_at at from quizzes z join subjects s on s.id = z.subject_id
          where s.teacher_id = :t and z.published = 1 and z.end_at is not null and z.end_at > :now order by z.end_at limit 6""", pn)
                .forEach(r -> deadlines.add(Map.of("kind", "QUIZ", "title", r.get("title"), "subject", r.get("subject"), "at", inst(r.get("at")), "link", "/teacher/quizzes/" + r.get("id") + "/attempts")));
        deadlines.sort(Comparator.comparing(m -> (Instant) m.get("at")));
        out.put("upcomingDeadlines", deadlines.stream().limit(6).toList());

        out.put("recentQuizActivity", q("""
                select qa.id, u.name student, z.title quiz, qa.score, qa.max_score, qa.status, qa.submitted_at at from quiz_attempts qa
                join quizzes z on z.id = qa.quiz_id join users u on u.id = qa.student_id join subjects s on s.id = z.subject_id
                where s.teacher_id = :t and qa.submitted_at is not null order by qa.submitted_at desc limit 8""", p).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("attemptId", r.get("id"));
            m.put("student", r.get("student"));
            m.put("quiz", r.get("quiz"));
            m.put("percent", dbl(r.get("max_score")) <= 0 ? 0 : round1(dbl(r.get("score")) * 100 / dbl(r.get("max_score"))));
            m.put("status", r.get("status"));
            m.put("at", inst(r.get("at")));
            return m;
        }).toList());
        return out;
    }

    private List<Map<String, Object>> engagement(List<Long> ids, List<Map<String, Object>> subjectRows) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Long> totals = publishedLessonTotals(ids);
        Map<Long, Map<Long, Long>> done = doneBySubjectStudent(ids);
        Map<String, Instant> last = lastActive(ids);
        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : subjectRows) {
            long sid = lng(s.get("id"));
            long total = totals.getOrDefault(sid, 0L);
            Map<Long, Long> students = done.getOrDefault(sid, Map.of());
            double sum = 0; int complete = 0, active = 0;
            for (var e : students.entrySet()) {
                double pct = total == 0 ? 0 : e.getValue() * 100.0 / total;
                sum += pct;
                if (total > 0 && e.getValue() >= total) complete++;
                Instant l = last.get(sid + ":" + e.getKey());
                if (l != null && l.isAfter(weekAgo)) active++;
            }
            int n = students.size();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectId", sid);
            m.put("title", s.get("title"));
            m.put("status", s.get("status"));
            m.put("students", n);
            m.put("averageProgress", n == 0 ? 0 : round1(sum / n));
            m.put("completionRate", n == 0 ? 0 : round1(complete * 100.0 / n));
            m.put("activeStudents", active);
            out.add(m);
        }
        return out;
    }

    // ---------------- per-subject analytics ----------------
    @Transactional(readOnly = true)
    public Map<String, Object> subjectAnalytics(Long subjectId) {
        access.requireOwnedSubject(subjectId);
        MapSqlParameterSource p = new MapSqlParameterSource("sid", subjectId);
        long total = publishedLessonTotals(List.of(subjectId)).getOrDefault(subjectId, 0L);
        Map<Long, Long> done = doneBySubjectStudent(List.of(subjectId)).getOrDefault(subjectId, Map.of());
        Map<String, Instant> last = lastActive(List.of(subjectId));
        Map<Long, Double> quizAvg = new HashMap<>();
        q("""
          select qa.student_id, avg(qa.score / qa.max_score * 100) a from quiz_attempts qa join quizzes z on z.id = qa.quiz_id
          where z.subject_id = :sid and qa.status = 'GRADED' and qa.max_score > 0 group by qa.student_id""", p)
                .forEach(r -> quizAvg.put(lng(r.get("student_id")), round1(dbl(r.get("a")))));
        Map<Long, Long> submitted = new HashMap<>();
        q("select sb.student_id, count(*) c from assignment_submissions sb join assignments a on a.id = sb.assignment_id where a.subject_id = :sid group by sb.student_id", p)
                .forEach(r -> submitted.put(lng(r.get("student_id")), lng(r.get("c"))));
        long assignmentTotal = lng(q("select count(*) c from assignments where subject_id = :sid", p).get(0).get("c"));
        Map<Long, Map<String, Object>> people = new HashMap<>();
        q("select u.id, u.name, u.email, e.enrolled_at from enrollments e join users u on u.id = e.student_id where e.subject_id = :sid and e.status = 'ACTIVE'", p)
                .forEach(r -> people.put(lng(r.get("id")), r));
        Instant now = Instant.now();
        List<Map<String, Object>> students = new ArrayList<>();
        double sum = 0; int complete = 0, active = 0, support = 0;
        for (var r : people.values()) {
            long sid = lng(r.get("id"));
            long d = done.getOrDefault(sid, 0L);
            double pct = total == 0 ? 0 : round1(d * 100.0 / total);
            Instant l = last.get(subjectId + ":" + sid);
            boolean isActive = l != null && l.isAfter(now.minus(Duration.ofDays(7)));
            Double qa = quizAvg.get(sid);
            Instant enrolledAt = inst(r.get("enrolled_at"));
            List<String> reasons = new ArrayList<>();
            if (qa != null && qa < 50) reasons.add("Quiz average below 50%");
            if (pct < 100 && ((l != null && l.isBefore(now.minus(Duration.ofDays(14)))) || (l == null && enrolledAt.isBefore(now.minus(Duration.ofDays(14)))))) reasons.add("Inactive for 14+ days");
            if (pct == 0 && enrolledAt.isBefore(now.minus(Duration.ofDays(7))) && total > 0) reasons.add("No progress after a week");
            sum += pct;
            if (total > 0 && d >= total) complete++;
            if (isActive) active++;
            if (!reasons.isEmpty()) support++;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", sid);
            m.put("name", r.get("name"));
            m.put("email", r.get("email"));
            m.put("progressPercent", pct);
            m.put("completedLessons", d);
            m.put("lastActiveAt", l);
            m.put("active", isActive);
            m.put("quizAverage", qa);
            m.put("assignmentsSubmitted", submitted.getOrDefault(sid, 0L));
            m.put("needsSupport", !reasons.isEmpty());
            m.put("supportReasons", reasons);
            students.add(m);
        }
        students.sort(Comparator.comparingDouble(m -> (Double) m.get("progressPercent")));
        int n = students.size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enrolled", n);
        out.put("publishedLessons", total);
        out.put("averageProgress", n == 0 ? 0 : round1(sum / n));
        out.put("completionRate", n == 0 ? 0 : round1(complete * 100.0 / n));
        out.put("activeStudents", active);
        out.put("inactiveStudents", n - active);
        out.put("needingSupport", support);
        out.put("assignmentTotal", assignmentTotal);
        int[] buckets = new int[4];
        for (var s : students) { double v = (Double) s.get("progressPercent"); buckets[v >= 100 ? 3 : (int) (v / 25)]++; }
        out.put("progressDistribution", List.of(Map.of("range", "0-24%", "students", buckets[0]), Map.of("range", "25-49%", "students", buckets[1]),
                Map.of("range", "50-99%", "students", buckets[2]), Map.of("range", "Completed", "students", buckets[3])));
        out.put("students", students);
        out.put("quizzes", q("""
                select z.id, z.title, z.passing_percentage pp, count(qa.id) attempts, avg(qa.score / qa.max_score * 100) a,
                       coalesce(sum(case when qa.score / qa.max_score * 100 >= z.passing_percentage then 1 else 0 end), 0) passed
                from quizzes z left join quiz_attempts qa on qa.quiz_id = z.id and qa.status = 'GRADED' and qa.max_score > 0
                where z.subject_id = :sid group by z.id, z.title, z.passing_percentage""", p).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            long at = lng(r.get("attempts"));
            m.put("quizId", r.get("id"));
            m.put("title", r.get("title"));
            m.put("gradedAttempts", at);
            m.put("averagePercent", r.get("a") == null ? null : round1(dbl(r.get("a"))));
            m.put("passRate", at == 0 ? null : round1(lng(r.get("passed")) * 100.0 / at));
            return m;
        }).toList());
        out.put("assignments", q("""
                select a.id, a.title, a.max_marks, count(sb.id) submitted, avg(sb.marks / a.max_marks * 100) a
                from assignments a left join assignment_submissions sb on sb.assignment_id = a.id where a.subject_id = :sid
                group by a.id, a.title, a.max_marks""", p).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assignmentId", r.get("id"));
            m.put("title", r.get("title"));
            m.put("submitted", lng(r.get("submitted")));
            m.put("enrolled", n);
            m.put("completionRate", n == 0 ? 0 : round1(lng(r.get("submitted")) * 100.0 / n));
            m.put("averageMarksPercent", r.get("a") == null ? null : round1(dbl(r.get("a"))));
            return m;
        }).toList());
        return out;
    }

    // ---------------- student ----------------
    private List<Map<String, Object>> enrolledSubjects(Long sid) {
        return q("""
                select s.id, s.title, s.category from enrollments e join subjects s on s.id = e.subject_id
                where e.student_id = :sid and e.status = 'ACTIVE' and s.status <> 'DRAFT' order by s.title""", new MapSqlParameterSource("sid", sid));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> studentProgress() {
        Long sid = CurrentUser.id();
        List<Map<String, Object>> subs = enrolledSubjects(sid);
        List<Long> ids = subs.stream().map(r -> lng(r.get("id"))).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            out.put("overallPercent", 0); out.put("completedLessons", 0); out.put("totalLessons", 0);
            out.put("subjects", List.of()); out.put("quizScores", List.of()); out.put("assignments", List.of()); out.put("deadlines", List.of());
            return out;
        }
        MapSqlParameterSource p = new MapSqlParameterSource("sid", sid).addValue("ids", ids);
        Map<Long, long[]> counts = new HashMap<>();
        q("""
          select l.subject_id, count(*) total, coalesce(sum(case when lp.completed = 1 then 1 else 0 end), 0) done
          from lessons l left join lesson_progress lp on lp.lesson_id = l.id and lp.student_id = :sid
          where l.subject_id in (:ids) and l.published = 1 group by l.subject_id""", p)
                .forEach(r -> counts.put(lng(r.get("subject_id")), new long[]{lng(r.get("total")), lng(r.get("done"))}));
        long totalAll = 0, doneAll = 0;
        List<Map<String, Object>> subjectOut = new ArrayList<>();
        for (var s : subs) {
            long[] c = counts.getOrDefault(lng(s.get("id")), new long[]{0, 0});
            totalAll += c[0]; doneAll += c[1];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectId", s.get("id"));
            m.put("title", s.get("title"));
            m.put("category", s.get("category"));
            m.put("totalLessons", c[0]);
            m.put("completedLessons", c[1]);
            m.put("pendingLessons", c[0] - c[1]);
            m.put("percent", c[0] == 0 ? 0 : round1(c[1] * 100.0 / c[0]));
            subjectOut.add(m);
        }
        out.put("overallPercent", totalAll == 0 ? 0 : round1(doneAll * 100.0 / totalAll));
        out.put("completedLessons", doneAll);
        out.put("totalLessons", totalAll);
        out.put("subjects", subjectOut);
        out.put("quizScores", q("""
                select z.id, z.title, s.title subject, s.id subject_id, count(qa.id) attempts, max(qa.score / qa.max_score * 100) best
                from quizzes z join subjects s on s.id = z.subject_id
                left join quiz_attempts qa on qa.quiz_id = z.id and qa.student_id = :sid and qa.status = 'GRADED' and qa.max_score > 0
                where z.subject_id in (:ids) and z.published = 1 group by z.id, z.title, s.title, s.id order by s.title, z.title""", p).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("quizId", r.get("id"));
            m.put("quiz", r.get("title"));
            m.put("subject", r.get("subject"));
            m.put("subjectId", r.get("subject_id"));
            m.put("attempts", lng(r.get("attempts")));
            m.put("bestPercent", r.get("best") == null ? null : round1(dbl(r.get("best"))));
            return m;
        }).toList());
        Instant now = Instant.now();
        List<Map<String, Object>> assignmentRows = q("""
                select a.id, a.title, s.title subject, s.id subject_id, a.max_marks, a.due_at, sb.marks, sb.submitted_at
                from assignments a join subjects s on s.id = a.subject_id
                left join assignment_submissions sb on sb.assignment_id = a.id and sb.student_id = :sid
                where a.subject_id in (:ids) order by a.due_at""", p);
        List<Map<String, Object>> asg = new ArrayList<>();
        List<Map<String, Object>> deadlines = new ArrayList<>();
        for (var r : assignmentRows) {
            Instant due = inst(r.get("due_at"));
            boolean submitted = r.get("submitted_at") != null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assignmentId", r.get("id"));
            m.put("title", r.get("title"));
            m.put("subject", r.get("subject"));
            m.put("subjectId", r.get("subject_id"));
            m.put("dueAt", due);
            m.put("maxMarks", dbl(r.get("max_marks")));
            m.put("marks", r.get("marks") == null ? null : dbl(r.get("marks")));
            m.put("status", r.get("marks") != null ? "GRADED" : submitted ? "SUBMITTED" : due.isBefore(now) ? "MISSED" : "PENDING");
            asg.add(m);
            if (!submitted && due.isAfter(now)) deadlines.add(Map.of("kind", "ASSIGNMENT", "title", r.get("title"), "subject", r.get("subject"), "at", due,
                    "link", "/student/subjects/" + r.get("subject_id") + "?tab=assignments"));
        }
        q("""
          select z.id, z.title, s.title subject, s.id subject_id, z.end_at, z.max_attempts,
                 (select count(*) from quiz_attempts qa where qa.quiz_id = z.id and qa.student_id = :sid) used
          from quizzes z join subjects s on s.id = z.subject_id
          where z.subject_id in (:ids) and z.published = 1 and z.end_at is not null""", p).forEach(r -> {
            Instant end = inst(r.get("end_at"));
            if (end.isAfter(now) && lng(r.get("used")) < lng(r.get("max_attempts"))) {
                deadlines.add(Map.of("kind", "QUIZ", "title", r.get("title"), "subject", r.get("subject"), "at", end,
                        "link", "/student/subjects/" + r.get("subject_id") + "?tab=quizzes"));
            }
        });
        deadlines.sort(Comparator.comparing(m -> (Instant) m.get("at")));
        out.put("assignments", asg);
        out.put("deadlines", deadlines);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> studentDashboard() {
        Map<String, Object> prog = studentProgress();
        Long sid = CurrentUser.id();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enrolledCourses", ((List<?>) prog.get("subjects")).size());
        out.put("overallPercent", prog.get("overallPercent"));
        out.put("completedLessons", prog.get("completedLessons"));
        out.put("totalLessons", prog.get("totalLessons"));
        List<Map<String, Object>> quizzes = (List<Map<String, Object>>) prog.get("quizScores");
        OptionalDouble avg = quizzes.stream().filter(m -> m.get("bestPercent") != null).mapToDouble(m -> (Double) m.get("bestPercent")).average();
        out.put("averageQuizScore", avg.isPresent() ? round1(avg.getAsDouble()) : null);
        List<?> deadlines = (List<?>) prog.get("deadlines");
        out.put("upcomingDeadlines", deadlines.stream().limit(6).toList());
        out.put("pendingDeadlines", deadlines.size());
        out.put("subjects", prog.get("subjects"));
        List<Map<String, Object>> cont = q("""
                select lp.lesson_id, lp.subject_id, l.title lesson, s.title subject from lesson_progress lp
                join lessons l on l.id = lp.lesson_id join subjects s on s.id = lp.subject_id
                join enrollments e on e.subject_id = lp.subject_id and e.student_id = lp.student_id and e.status = 'ACTIVE'
                where lp.student_id = :sid and l.published = 1 and s.status <> 'DRAFT' order by lp.updated_at desc limit 1""", new MapSqlParameterSource("sid", sid));
        out.put("continueLearning", cont.isEmpty() ? null : Map.of("lessonId", cont.get(0).get("lesson_id"), "subjectId", cont.get(0).get("subject_id"),
                "lesson", cont.get(0).get("lesson"), "subject", cont.get(0).get("subject")));
        return out;
    }
}
