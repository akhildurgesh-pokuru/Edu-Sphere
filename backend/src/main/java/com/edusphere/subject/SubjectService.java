package com.edusphere.subject;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.common.PageResponse;
import com.edusphere.domain.Enrollment;
import com.edusphere.domain.Subject;
import com.edusphere.notification.NotificationService;
import com.edusphere.progress.ProgressService;
import com.edusphere.repository.EnrollmentRepository;
import com.edusphere.repository.LessonRepository;
import com.edusphere.repository.SubjectRepository;
import com.edusphere.storage.FileValidator;
import com.edusphere.storage.StorageService;
import com.edusphere.subject.SubjectDtos.*;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubjectService {
    public static final int CATALOG_PAGE_SIZE = 12;
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final LessonRepository lessons;
    private final UserRepository users;
    private final AccessService access;
    private final ProgressService progress;
    private final NotificationService notifications;
    private final StorageService storage;
    private final FileValidator validator;
    private final NamedParameterJdbcTemplate jdbc;

    public SubjectService(SubjectRepository subjects, EnrollmentRepository enrollments, LessonRepository lessons, UserRepository users,
                          AccessService access, ProgressService progress, NotificationService notifications,
                          StorageService storage, FileValidator validator, NamedParameterJdbcTemplate jdbc) {
        this.subjects = subjects; this.enrollments = enrollments; this.lessons = lessons; this.users = users;
        this.access = access; this.progress = progress; this.notifications = notifications;
        this.storage = storage; this.validator = validator; this.jdbc = jdbc;
    }

    /** Maps a whole page of subjects with a fixed number of queries (no N+1). */
    @Transactional(readOnly = true)
    public List<SubjectResponse> toResponses(List<Subject> list, Long viewerStudentId) {
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(Subject::getId).toList();
        Map<Long, String> teacherNames = users.findAllById(list.stream().map(Subject::getTeacherId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, Long> counts = new HashMap<>();
        enrollments.countActiveBySubjects(ids).forEach(r -> counts.put((Long) r[0], (Long) r[1]));
        Map<Long, Long> lessonCounts = new HashMap<>();
        jdbc.query("select subject_id, count(*) c from lessons where subject_id in (:ids) and published = 1 group by subject_id",
                new MapSqlParameterSource("ids", ids), rs -> { lessonCounts.put(rs.getLong(1), rs.getLong(2)); });
        Set<Long> enrolledIds = new HashSet<>();
        Map<Long, Double> pct = Map.of();
        if (viewerStudentId != null) {
            enrollments.findByStudentIdAndSubjectIdInAndStatus(viewerStudentId, ids, "ACTIVE").forEach(e -> enrolledIds.add(e.getSubjectId()));
            pct = progress.percentForSubjects(viewerStudentId, enrolledIds);
        }
        List<SubjectResponse> out = new ArrayList<>();
        for (Subject s : list) {
            boolean enrolled = enrolledIds.contains(s.getId());
            out.add(new SubjectResponse(s.getId(), s.getTitle(), s.getDescription(), s.getCategory(), s.getStatus(), s.getTeacherId(),
                    teacherNames.getOrDefault(s.getTeacherId(), "Teacher"), counts.getOrDefault(s.getId(), 0L),
                    lessonCounts.getOrDefault(s.getId(), 0L), viewerStudentId == null ? null : enrolled,
                    s.getCoverImagePath() != null, enrolled ? pct.getOrDefault(s.getId(), 0.0) : null,
                    s.getCreatedAt(), s.getUpdatedAt(), s.getPublishedAt()));
        }
        return out;
    }

    // ---------- teacher ----------
    @Transactional
    public SubjectResponse create(SubjectRequest req) {
        Subject s = new Subject();
        s.setTeacherId(CurrentUser.id());
        apply(s, req);
        return toResponses(List.of(subjects.save(s)), null).get(0);
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest req) {
        Subject s = access.requireOwnedSubject(id);
        apply(s, req);
        s.setUpdatedAt(Instant.now());
        return toResponses(List.of(subjects.save(s)), null).get(0);
    }

    private void apply(Subject s, SubjectRequest req) {
        s.setTitle(req.title().trim());
        s.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        s.setCategory(req.category() == null || req.category().isBlank() ? null : req.category().trim());
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listOwn() {
        return toResponses(subjects.findByTeacherIdOrderByUpdatedAtDesc(CurrentUser.id()), null);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getOwn(Long id) {
        return toResponses(List.of(access.requireOwnedSubject(id)), null).get(0);
    }

    @Transactional
    public SubjectResponse setStatus(Long id, String status) {
        Subject s = access.requireOwnedSubject(id);
        String st = status == null ? "" : status.trim().toUpperCase();
        if (!STATUSES.contains(st)) throw ApiException.badRequest("Status must be DRAFT, PUBLISHED or ARCHIVED");
        if ("PUBLISHED".equals(st)) {
            if (lessons.countBySubjectIdAndPublishedTrue(id) == 0) {
                throw ApiException.badRequest("Publish at least one lesson before publishing the subject");
            }
            if (s.getPublishedAt() == null) s.setPublishedAt(Instant.now());
        }
        s.setStatus(st);
        s.setUpdatedAt(Instant.now());
        return toResponses(List.of(subjects.save(s)), null).get(0);
    }

    @Transactional
    public SubjectResponse setCover(Long id, MultipartFile file) {
        Subject s = access.requireOwnedSubject(id);
        FileValidator.FileInfo info = validator.validate(file, FileValidator.IMAGE_EXT);
        String key;
        try (var in = file.getInputStream()) {
            key = storage.store(in, info.extension());
        } catch (IOException e) {
            throw ApiException.badRequest("The file could not be saved");
        }
        String old = s.getCoverImagePath();
        s.setCoverImagePath(key);
        s.setUpdatedAt(Instant.now());
        subjects.save(s);
        if (old != null) storage.delete(old);
        return toResponses(List.of(s), null).get(0);
    }

    public record CoverFile(Resource resource, String contentType) {}

    @Transactional(readOnly = true)
    public CoverFile cover(Long id) {
        var u = CurrentUser.get();
        Subject s = subjects.findById(id).orElseThrow(() -> ApiException.notFound("Not found"));
        boolean ok = s.getTeacherId().equals(u.getId()) || "PUBLISHED".equals(s.getStatus())
                || (u.isStudent() && access.isActiveEnrolled(id, u.getId()) && !"DRAFT".equals(s.getStatus()));
        if (!ok || s.getCoverImagePath() == null) throw ApiException.notFound("Not found");
        String key = s.getCoverImagePath();
        String ext = key.substring(key.lastIndexOf('.') + 1);
        String ct = switch (ext) { case "png" -> "image/png"; case "gif" -> "image/gif"; case "webp" -> "image/webp"; default -> "image/jpeg"; };
        return new CoverFile(storage.load(key), ct);
    }

    @Transactional(readOnly = true)
    public List<StudentRow> students(Long subjectId) {
        access.requireOwnedSubject(subjectId);
        List<Enrollment> list = enrollments.findBySubjectIdAndStatus(subjectId, "ACTIVE");
        Map<Long, User> byId = users.findAllById(list.stream().map(Enrollment::getStudentId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        long total = lessons.countBySubjectIdAndPublishedTrue(subjectId);
        Map<Long, Long> done = new HashMap<>();
        jdbc.query("""
                select lp.student_id, count(*) c from lesson_progress lp join lessons l on l.id = lp.lesson_id
                where lp.subject_id = :sid and lp.completed = 1 and l.published = 1 group by lp.student_id""",
                new MapSqlParameterSource("sid", subjectId), rs -> { done.put(rs.getLong(1), rs.getLong(2)); });
        return list.stream().map(e -> {
            User u = byId.get(e.getStudentId());
            double pct = total == 0 ? 0 : Math.round(done.getOrDefault(e.getStudentId(), 0L) * 1000.0 / total) / 10.0;
            return new StudentRow(e.getStudentId(), u == null ? "Student" : u.getName(), u == null ? "" : u.getEmail(), e.getEnrolledAt(), pct);
        }).sorted(Comparator.comparing(StudentRow::enrolledAt).reversed()).toList();
    }

    @Transactional
    public void removeStudent(Long subjectId, Long studentId) {
        Subject s = access.requireOwnedSubject(subjectId);
        Enrollment e = enrollments.findBySubjectIdAndStudentId(subjectId, studentId)
                .orElseThrow(() -> ApiException.notFound("Student is not enrolled"));
        e.setStatus("REMOVED");
        enrollments.save(e);
        notifications.notify(studentId, "ENROLLMENT", "Removed from " + s.getTitle(), "Your teacher removed you from this course.", "/student/catalog", null);
    }

    // ---------- student ----------
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> catalog(String q, String category, int page) {
        String query = q == null || q.isBlank() ? null : q.trim();
        String cat = category == null || category.isBlank() ? null : category.trim();
        var p = subjects.searchCatalog(query, cat, PageRequest.of(Math.max(page, 0), CATALOG_PAGE_SIZE, org.springframework.data.domain.Sort.by("publishedAt").descending().and(org.springframework.data.domain.Sort.by("id").descending())));
        return PageResponse.of(p, l -> toResponses(l, CurrentUser.id()));
    }

    @Transactional(readOnly = true)
    public List<String> categories() { return subjects.publishedCategories(); }

    @Transactional(readOnly = true)
    public List<SubjectResponse> enrolledSubjects() {
        Long sid = CurrentUser.id();
        List<Long> ids = enrollments.findByStudentIdAndStatus(sid, "ACTIVE").stream().map(Enrollment::getSubjectId).toList();
        List<Subject> list = subjects.findAllById(ids).stream().filter(s -> !"DRAFT".equals(s.getStatus()))
                .sorted(Comparator.comparing(Subject::getTitle, String.CASE_INSENSITIVE_ORDER)).toList();
        return toResponses(list, sid);
    }

    @Transactional(readOnly = true)
    public SubjectResponse studentDetail(Long id) {
        Long sid = CurrentUser.id();
        Subject s = subjects.findById(id).orElseThrow(() -> ApiException.notFound("Course not found"));
        boolean enrolled = access.isActiveEnrolled(id, sid);
        boolean visible = "PUBLISHED".equals(s.getStatus()) || (enrolled && "ARCHIVED".equals(s.getStatus()));
        if (!visible) throw ApiException.notFound("Course not found");
        return toResponses(List.of(s), sid).get(0);
    }

    @Transactional
    public SubjectResponse enroll(Long id) {
        Long sid = CurrentUser.id();
        Subject s = subjects.findById(id).filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Course not found"));
        Optional<Enrollment> existing = enrollments.findBySubjectIdAndStudentId(id, sid);
        if (existing.isPresent()) {
            Enrollment e = existing.get();
            if ("ACTIVE".equals(e.getStatus())) throw ApiException.conflict("You are already enrolled in this course");
            if ("REMOVED".equals(e.getStatus())) throw ApiException.forbidden("You can no longer enroll in this course");
            e.setStatus("ACTIVE");
            e.setEnrolledAt(Instant.now());
            enrollments.save(e);
        } else {
            Enrollment e = new Enrollment();
            e.setSubjectId(id);
            e.setStudentId(sid);
            enrollments.save(e);
        }
        notifications.notify(sid, "ENROLLMENT", "You're enrolled in " + s.getTitle(), "Start learning whenever you're ready.", "/student/subjects/" + id, null);
        notifications.notify(s.getTeacherId(), "ENROLLMENT", "New student enrolled", CurrentUser.get().getName() + " joined " + s.getTitle(),
                "/teacher/subjects/" + id + "?tab=students", null);
        return toResponses(List.of(s), sid).get(0);
    }

    @Transactional
    public void leave(Long id) {
        Enrollment e = enrollments.findBySubjectIdAndStudentId(id, CurrentUser.id())
                .filter(x -> "ACTIVE".equals(x.getStatus())).orElseThrow(() -> ApiException.notFound("You are not enrolled in this course"));
        e.setStatus("LEFT");
        enrollments.save(e);
    }
}
