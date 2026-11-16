package com.edusphere.progress;

import com.edusphere.common.ApiException;
import com.edusphere.domain.Lesson;
import com.edusphere.domain.LessonProgress;
import com.edusphere.repository.LessonProgressRepository;
import com.edusphere.repository.LessonRepository;
import com.edusphere.subject.AccessService;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgressService {
    public record LessonProgressDto(Long lessonId, boolean completed, int lastPositionSeconds, int furthestPositionSeconds, String notes) {}
    public record SubjectProgress(Long subjectId, int totalLessons, int completedLessons, double percent, List<LessonProgressDto> lessons) {}

    private static final int MAX_POSITION = 12 * 3600;

    private final LessonRepository lessons;
    private final LessonProgressRepository progress;
    private final AccessService access;
    private final NamedParameterJdbcTemplate jdbc;

    public ProgressService(LessonRepository lessons, LessonProgressRepository progress, AccessService access, NamedParameterJdbcTemplate jdbc) {
        this.lessons = lessons; this.progress = progress; this.access = access; this.jdbc = jdbc;
    }

    /** Subject progress = completed published lessons / total published lessons (0 when there are none). */
    public Map<Long, Double> percentForSubjects(Long studentId, Collection<Long> subjectIds) {
        Map<Long, Double> out = new HashMap<>();
        if (subjectIds.isEmpty()) return out;
        jdbc.query("""
                select l.subject_id, count(*) total, coalesce(sum(case when lp.completed = 1 then 1 else 0 end), 0) done
                from lessons l left join lesson_progress lp on lp.lesson_id = l.id and lp.student_id = :sid
                where l.subject_id in (:ids) and l.published = 1 group by l.subject_id""",
                new MapSqlParameterSource("sid", studentId).addValue("ids", subjectIds),
                rs -> {
                    long total = rs.getLong("total");
                    out.put(rs.getLong("subject_id"), total == 0 ? 0.0 : Math.round(rs.getLong("done") * 1000.0 / total) / 10.0);
                });
        subjectIds.forEach(id -> out.putIfAbsent(id, 0.0));
        return out;
    }

    @Transactional(readOnly = true)
    public SubjectProgress forSubject(Long subjectId, Long studentId) {
        List<Lesson> published = lessons.findBySubjectIdAndPublishedTrue(subjectId);
        Map<Long, LessonProgress> byLesson = new HashMap<>();
        progress.findBySubjectIdAndStudentId(subjectId, studentId).forEach(p -> byLesson.put(p.getLessonId(), p));
        int done = 0;
        List<LessonProgressDto> dtos = new java.util.ArrayList<>();
        for (Lesson l : published) {
            LessonProgress p = byLesson.get(l.getId());
            if (p != null && p.isCompleted()) done++;
            dtos.add(p == null ? new LessonProgressDto(l.getId(), false, 0, 0, null)
                    : new LessonProgressDto(l.getId(), p.isCompleted(), p.getLastPositionSeconds(), p.getFurthestPositionSeconds(), p.getNotes()));
        }
        double pct = published.isEmpty() ? 0 : Math.round(done * 1000.0 / published.size()) / 10.0;
        return new SubjectProgress(subjectId, published.size(), done, pct, dtos);
    }

    private Lesson requirePublishedLesson(Long lessonId, Long studentId) {
        Lesson l = lessons.findById(lessonId).orElseThrow(() -> ApiException.notFound("Lesson not found"));
        access.requireEnrolledStudent(l.getSubjectId());
        if (!l.isPublished()) throw ApiException.notFound("Lesson not found");
        return l;
    }

    private LessonProgress row(Lesson l, Long studentId) {
        return progress.findByLessonIdAndStudentId(l.getId(), studentId).orElseGet(() -> {
            LessonProgress p = new LessonProgress();
            p.setLessonId(l.getId());
            p.setStudentId(studentId);
            p.setSubjectId(l.getSubjectId());
            return p;
        });
    }

    /** The server clamps the reported position; it never trusts a client-sent percentage. */
    @Transactional
    public LessonProgressDto savePosition(Long lessonId, Long studentId, int position) {
        Lesson l = requirePublishedLesson(lessonId, studentId);
        int max = l.getDurationSeconds() != null && l.getDurationSeconds() > 0 ? l.getDurationSeconds() : MAX_POSITION;
        int pos = Math.max(0, Math.min(position, max));
        LessonProgress p = row(l, studentId);
        p.setLastPositionSeconds(pos);
        p.setFurthestPositionSeconds(Math.max(p.getFurthestPositionSeconds(), pos));
        p.setUpdatedAt(Instant.now());
        return dto(progress.save(p));
    }

    @Transactional
    public LessonProgressDto setCompleted(Long lessonId, Long studentId, boolean completed) {
        Lesson l = requirePublishedLesson(lessonId, studentId);
        LessonProgress p = row(l, studentId);
        p.setCompleted(completed);
        p.setCompletedAt(completed ? Instant.now() : null);
        p.setUpdatedAt(Instant.now());
        return dto(progress.save(p));
    }

    @Transactional
    public LessonProgressDto saveNotes(Long lessonId, Long studentId, String notes) {
        Lesson l = requirePublishedLesson(lessonId, studentId);
        LessonProgress p = row(l, studentId);
        p.setNotes(notes == null || notes.isBlank() ? null : notes.substring(0, Math.min(notes.length(), 10000)));
        p.setUpdatedAt(Instant.now());
        return dto(progress.save(p));
    }

    public static LessonProgressDto dto(LessonProgress p) {
        return new LessonProgressDto(p.getLessonId(), p.isCompleted(), p.getLastPositionSeconds(), p.getFurthestPositionSeconds(), p.getNotes());
    }
}
