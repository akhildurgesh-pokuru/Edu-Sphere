package com.edusphere.notification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly: reminds enrolled students about assignments and quizzes that close within 24 hours (once per user and item). */
@Component
public class DeadlineReminderJob {
    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderJob.class);
    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationService notifications;

    public DeadlineReminderJob(NamedParameterJdbcTemplate jdbc, NotificationService notifications) {
        this.jdbc = jdbc; this.notifications = notifications;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    public void run() {
        try {
            Instant now = Instant.now();
            var p = new MapSqlParameterSource("now", Timestamp.from(now)).addValue("soon", Timestamp.from(now.plus(Duration.ofHours(24))));
            List<Map<String, Object>> a = jdbc.queryForList("""
                    select a.id, a.title, a.subject_id, e.student_id from assignments a join enrollments e on e.subject_id = a.subject_id and e.status = 'ACTIVE'
                    where a.due_at > :now and a.due_at <= :soon
                      and not exists (select 1 from assignment_submissions sb where sb.assignment_id = a.id and sb.student_id = e.student_id)""", p);
            for (var r : a) {
                notifications.notify(((Number) r.get("student_id")).longValue(), "UPCOMING_DEADLINE", "Assignment due soon", (String) r.get("title"),
                        "/student/subjects/" + r.get("subject_id") + "?tab=assignments", "due-assignment-" + r.get("id"));
            }
            List<Map<String, Object>> z = jdbc.queryForList("""
                    select z.id, z.title, z.subject_id, z.max_attempts, e.student_id,
                      (select count(*) from quiz_attempts qa where qa.quiz_id = z.id and qa.student_id = e.student_id) used
                    from quizzes z join enrollments e on e.subject_id = z.subject_id and e.status = 'ACTIVE'
                    where z.published = 1 and z.end_at > :now and z.end_at <= :soon""", p);
            for (var r : z) {
                if (((Number) r.get("used")).intValue() >= ((Number) r.get("max_attempts")).intValue()) continue;
                notifications.notify(((Number) r.get("student_id")).longValue(), "UPCOMING_DEADLINE", "Quiz closes soon", (String) r.get("title"),
                        "/student/subjects/" + r.get("subject_id") + "?tab=quizzes", "due-quiz-" + r.get("id"));
            }
        } catch (Exception e) {
            log.warn("Deadline reminder run failed: {}", e.getMessage());
        }
    }
}
