package com.edusphere.content;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.content.ContentDtos.*;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.progress.ProgressService;
import com.edusphere.repository.*;
import com.edusphere.security.AppUserDetails;
import com.edusphere.storage.FileValidator;
import com.edusphere.storage.StorageService;
import com.edusphere.subject.AccessService;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentService {
    private static final Set<String> LESSON_TYPES = Set.of("VIDEO", "TEXT", "DOCUMENT", "LINK");

    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LearningResourceRepository resources;
    private final LessonProgressRepository progressRepo;
    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final AccessService access;
    private final NotificationService notifications;
    private final ProgressService progressService;
    private final StorageService storage;
    private final FileValidator validator;

    public ContentService(CourseModuleRepository modules, LessonRepository lessons, LearningResourceRepository resources,
                          LessonProgressRepository progressRepo, SubjectRepository subjects, EnrollmentRepository enrollments,
                          AccessService access, NotificationService notifications, ProgressService progressService,
                          StorageService storage, FileValidator validator) {
        this.modules = modules; this.lessons = lessons; this.resources = resources; this.progressRepo = progressRepo;
        this.subjects = subjects; this.enrollments = enrollments; this.access = access; this.notifications = notifications;
        this.progressService = progressService; this.storage = storage; this.validator = validator;
    }

    public static ResourceDto toDto(LearningResource r) {
        return new ResourceDto(r.getId(), r.getOriginalName(), r.getKind(), r.getContentType(), r.getSizeBytes(), "/api/resources/" + r.getId());
    }

    // ---------- teacher: curriculum ----------
    @Transactional(readOnly = true)
    public CurriculumResponse teacherCurriculum(Long subjectId) {
        access.requireOwnedSubject(subjectId);
        return build(subjectId, false, true, Set.of());
    }

    private CurriculumResponse build(Long subjectId, boolean locked, boolean includeDrafts, Set<Long> completed) {
        List<Lesson> all = lessons.findBySubjectIdOrderByPositionAscIdAsc(subjectId);
        Map<Long, List<Lesson>> byModule = all.stream().filter(l -> includeDrafts || l.isPublished())
                .collect(Collectors.groupingBy(Lesson::getModuleId));
        List<CurriculumModule> out = new ArrayList<>();
        for (CourseModule m : modules.findBySubjectIdOrderByPositionAscIdAsc(subjectId)) {
            List<CurriculumLesson> ls = byModule.getOrDefault(m.getId(), List.of()).stream()
                    .map(l -> new CurriculumLesson(l.getId(), l.getTitle(), l.getType(), l.getDurationSeconds(), l.getPosition(),
                            l.isPublished(), completed.contains(l.getId()), l.getResourceId() != null)).toList();
            if (!includeDrafts && ls.isEmpty()) continue;
            out.add(new CurriculumModule(m.getId(), m.getTitle(), m.getDescription(), m.getPosition(), ls));
        }
        return new CurriculumResponse(subjectId, locked, out);
    }

    @Transactional
    public CurriculumModule createModule(Long subjectId, ModuleRequest req) {
        access.requireOwnedSubject(subjectId);
        CourseModule m = new CourseModule();
        m.setSubjectId(subjectId);
        m.setTitle(req.title().trim());
        m.setDescription(blankToNull(req.description()));
        m.setPosition(modules.findBySubjectIdOrderByPositionAscIdAsc(subjectId).size());
        m = modules.save(m);
        return new CurriculumModule(m.getId(), m.getTitle(), m.getDescription(), m.getPosition(), List.of());
    }

    private CourseModule ownedModule(Long moduleId) {
        CourseModule m = modules.findById(moduleId).orElseThrow(() -> ApiException.notFound("Module not found"));
        access.requireOwnedSubject(m.getSubjectId());
        return m;
    }

    @Transactional
    public CurriculumModule updateModule(Long moduleId, ModuleRequest req) {
        CourseModule m = ownedModule(moduleId);
        m.setTitle(req.title().trim());
        m.setDescription(blankToNull(req.description()));
        modules.save(m);
        return new CurriculumModule(m.getId(), m.getTitle(), m.getDescription(), m.getPosition(), List.of());
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        CourseModule m = ownedModule(moduleId);
        List<Lesson> ls = lessons.findByModuleIdOrderByPositionAscIdAsc(moduleId);
        modules.delete(m);
        modules.flush();
        ls.forEach(l -> cleanupResource(l.getResourceId()));
    }

    @Transactional
    public void reorderModules(Long subjectId, List<Long> ids) {
        access.requireOwnedSubject(subjectId);
        List<CourseModule> existing = modules.findBySubjectIdOrderByPositionAscIdAsc(subjectId);
        requireSameIds(existing.stream().map(CourseModule::getId).toList(), ids);
        Map<Long, CourseModule> byId = existing.stream().collect(Collectors.toMap(CourseModule::getId, x -> x));
        for (int i = 0; i < ids.size(); i++) byId.get(ids.get(i)).setPosition(i);
        modules.saveAll(existing);
    }

    private static void requireSameIds(List<Long> existing, List<Long> given) {
        if (given.size() != existing.size() || !new HashSet<>(given).equals(new HashSet<>(existing))) {
            throw ApiException.badRequest("The list of ids must match the current items exactly");
        }
    }

    // ---------- teacher: lessons ----------
    @Transactional
    public LessonDetail createLesson(Long moduleId, LessonRequest req) {
        CourseModule m = ownedModule(moduleId);
        Lesson l = new Lesson();
        l.setModuleId(moduleId);
        l.setSubjectId(m.getSubjectId());
        l.setPosition(lessons.findByModuleIdOrderByPositionAscIdAsc(moduleId).size());
        apply(l, req);
        l = lessons.save(l);
        maybeNotify(l);
        return detailFor(l, null);
    }

    @Transactional
    public LessonDetail updateLesson(Long lessonId, LessonRequest req) {
        Lesson l = ownedLesson(lessonId);
        boolean wasPublished = l.isPublished();
        Long oldResource = l.getResourceId();
        apply(l, req);
        l.setUpdatedAt(Instant.now());
        l = lessons.save(l);
        if (oldResource != null && !oldResource.equals(l.getResourceId())) {
            lessons.flush();
            cleanupResource(oldResource);
        }
        if (!wasPublished) maybeNotify(l);
        return detailFor(l, null);
    }

    private Lesson ownedLesson(Long lessonId) {
        Lesson l = lessons.findById(lessonId).orElseThrow(() -> ApiException.notFound("Lesson not found"));
        access.requireOwnedSubject(l.getSubjectId());
        return l;
    }

    private void apply(Lesson l, LessonRequest req) {
        String type = req.type().trim().toUpperCase();
        if (!LESSON_TYPES.contains(type)) throw ApiException.badRequest("Lesson type must be VIDEO, TEXT, DOCUMENT or LINK");
        l.setTitle(req.title().trim());
        l.setType(type);
        l.setDurationSeconds(req.durationSeconds() == null ? 0 : Math.max(0, Math.min(req.durationSeconds(), 12 * 3600)));
        l.setPublished(Boolean.TRUE.equals(req.published()));
        l.setContentText(null);
        l.setExternalUrl(null);
        l.setResourceId(null);
        switch (type) {
            case "TEXT" -> {
                if (req.contentText() == null || req.contentText().isBlank()) throw ApiException.badRequest("Write the lesson text");
                l.setContentText(req.contentText());
            }
            case "LINK" -> {
                String u = req.externalUrl() == null ? "" : req.externalUrl().trim();
                if (!u.matches("(?i)^https?://[^\\s]+$")) throw ApiException.badRequest("Enter a valid http(s) link");
                l.setExternalUrl(u);
                l.setContentText(blankToNull(req.contentText()));
            }
            default -> {
                if (req.resourceId() == null) throw ApiException.badRequest("Upload a file for this lesson first");
                LearningResource r = resources.findById(req.resourceId()).filter(x -> x.getSubjectId().equals(l.getSubjectId()))
                        .orElseThrow(() -> ApiException.badRequest("The uploaded file was not found"));
                boolean video = "VIDEO".equals(r.getKind());
                if ("VIDEO".equals(type) != video) {
                    throw ApiException.badRequest(video ? "Use a document lesson for this file" : "A video lesson needs an MP4 or WebM file");
                }
                l.setResourceId(r.getId());
                l.setContentText(blankToNull(req.contentText()));
            }
        }
    }

    private void maybeNotify(Lesson l) {
        if (!l.isPublished()) return;
        Subject s = subjects.findById(l.getSubjectId()).orElse(null);
        if (s == null || !"PUBLISHED".equals(s.getStatus())) return;
        for (Enrollment e : enrollments.findBySubjectIdAndStatus(s.getId(), "ACTIVE")) {
            notifications.notify(e.getStudentId(), "NEW_MATERIAL", "New lesson in " + s.getTitle(), l.getTitle(),
                    "/student/subjects/" + s.getId() + "/learn/" + l.getId(), "lesson-" + l.getId());
        }
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson l = ownedLesson(lessonId);
        Long rid = l.getResourceId();
        lessons.delete(l);
        lessons.flush();
        cleanupResource(rid);
    }

    @Transactional
    public void reorderLessons(Long moduleId, List<Long> ids) {
        ownedModule(moduleId);
        List<Lesson> existing = lessons.findByModuleIdOrderByPositionAscIdAsc(moduleId);
        requireSameIds(existing.stream().map(Lesson::getId).toList(), ids);
        Map<Long, Lesson> byId = existing.stream().collect(Collectors.toMap(Lesson::getId, x -> x));
        for (int i = 0; i < ids.size(); i++) byId.get(ids.get(i)).setPosition(i);
        lessons.saveAll(existing);
    }

    private void cleanupResource(Long resourceId) {
        if (resourceId == null || !lessons.findByResourceId(resourceId).isEmpty()) return;
        resources.findById(resourceId).ifPresent(r -> {
            resources.delete(r);
            storage.delete(r.getStoredName());
        });
    }

    // ---------- teacher: uploads ----------
    @Transactional
    public ResourceDto upload(Long subjectId, MultipartFile file) {
        access.requireOwnedSubject(subjectId);
        return toDto(storeResource(subjectId, file, FileValidator.MATERIAL_EXT, null));
    }

    /** Validates and stores a file; kindOverride lets assignments mark files as ATTACHMENT or SUBMISSION. */
    @Transactional
    public LearningResource storeResource(Long subjectId, MultipartFile file, Set<String> allowedExt, String kindOverride) {
        FileValidator.FileInfo info = validator.validate(file, allowedExt);
        String key;
        try (var in = file.getInputStream()) {
            key = storage.store(in, info.extension());
        } catch (IOException e) {
            throw ApiException.badRequest("The file could not be saved");
        }
        LearningResource r = new LearningResource();
        r.setSubjectId(subjectId);
        r.setOwnerId(CurrentUser.id());
        r.setOriginalName(info.originalName());
        r.setStoredName(key);
        r.setContentType(info.contentType());
        r.setSizeBytes(info.size());
        r.setKind(kindOverride != null ? kindOverride : info.kind());
        return resources.save(r);
    }

    // ---------- student view ----------
    @Transactional(readOnly = true)
    public CurriculumResponse studentCurriculum(Long subjectId) {
        AppUserDetails u = CurrentUser.get();
        Subject s = subjects.findById(subjectId).orElseThrow(() -> ApiException.notFound("Course not found"));
        boolean enrolled = access.isActiveEnrolled(subjectId, u.getId());
        boolean visible = "PUBLISHED".equals(s.getStatus()) || (enrolled && "ARCHIVED".equals(s.getStatus()));
        if (!visible) throw ApiException.notFound("Course not found");
        Set<Long> done = enrolled ? progressRepo.findBySubjectIdAndStudentId(subjectId, u.getId()).stream()
                .filter(LessonProgress::isCompleted).map(LessonProgress::getLessonId).collect(Collectors.toSet()) : Set.of();
        return build(subjectId, !enrolled, false, done);
    }

    @Transactional(readOnly = true)
    public LessonDetail lesson(Long lessonId) {
        Lesson l = lessons.findById(lessonId).orElseThrow(() -> ApiException.notFound("Lesson not found"));
        AccessService.Access a = access.requireAccess(l.getSubjectId());
        if (!a.owner() && !l.isPublished()) throw ApiException.notFound("Lesson not found");
        ProgressService.LessonProgressDto prog = null;
        if (!a.owner()) {
            prog = progressRepo.findByLessonIdAndStudentId(lessonId, CurrentUser.id()).map(ProgressService::dto)
                    .orElse(new ProgressService.LessonProgressDto(lessonId, false, 0, 0, null));
        }
        return detailFor(l, prog);
    }

    private LessonDetail detailFor(Lesson l, ProgressService.LessonProgressDto progress) {
        ResourceDto res = l.getResourceId() == null ? null : resources.findById(l.getResourceId()).map(ContentService::toDto).orElse(null);
        return new LessonDetail(l.getId(), l.getSubjectId(), l.getModuleId(), l.getTitle(), l.getType(), l.getContentText(), l.getExternalUrl(),
                res, l.getDurationSeconds(), l.isPublished(), progress);
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
