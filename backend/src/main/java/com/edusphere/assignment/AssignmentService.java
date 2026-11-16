package com.edusphere.assignment;

import com.edusphere.assignment.AssignmentDtos.*;
import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.content.ContentDtos.ResourceDto;
import com.edusphere.content.ContentService;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.repository.*;
import com.edusphere.storage.FileValidator;
import com.edusphere.storage.StorageService;
import com.edusphere.subject.AccessService;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssignmentService {
    private final AssignmentRepository assignments;
    private final AssignmentSubmissionRepository submissions;
    private final LearningResourceRepository resources;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AccessService access;
    private final ContentService content;
    private final NotificationService notifications;
    private final StorageService storage;

    public AssignmentService(AssignmentRepository assignments, AssignmentSubmissionRepository submissions, LearningResourceRepository resources,
                             EnrollmentRepository enrollments, UserRepository users, AccessService access, ContentService content,
                             NotificationService notifications, StorageService storage) {
        this.assignments = assignments; this.submissions = submissions; this.resources = resources; this.enrollments = enrollments;
        this.users = users; this.access = access; this.content = content; this.notifications = notifications; this.storage = storage;
    }

    private Assignment ownedAssignment(Long id) {
        Assignment a = assignments.findById(id).orElseThrow(() -> ApiException.notFound("Assignment not found"));
        access.requireOwnedSubject(a.getSubjectId());
        return a;
    }

    private static List<String> types(Assignment a) {
        return Arrays.stream(a.getAllowedTypes().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private ResourceDto res(Long id) {
        return id == null ? null : resources.findById(id).map(ContentService::toDto).orElse(null);
    }

    private SubmissionDto sub(AssignmentSubmission s) {
        return s == null ? null : new SubmissionDto(s.getId(), res(s.getResourceId()), s.getSubmittedAt(), s.isLate(), s.getMarks(), s.getFeedback(), s.getGradedAt());
    }

    private AssignmentDto dto(Assignment a, AssignmentSubmission mine, Long count, Long pending) {
        return new AssignmentDto(a.getId(), a.getSubjectId(), a.getTitle(), a.getDescription(), a.getInstructions(), a.getDueAt(), a.getMaxMarks(),
                types(a), res(a.getAttachmentResourceId()), a.getCreatedAt(), Instant.now().isAfter(a.getDueAt()), sub(mine), count, pending);
    }

    private void apply(Assignment a, AssignmentRequest r) {
        List<String> req = r.allowedTypes() == null || r.allowedTypes().isEmpty() ? List.of("pdf", "doc", "docx", "ppt", "pptx", "txt") : r.allowedTypes();
        List<String> clean = req.stream().map(x -> x.trim().toLowerCase().replace(".", "")).distinct().toList();
        if (!FileValidator.DOCUMENT_EXT.containsAll(clean)) {
            throw ApiException.badRequest("Allowed file types: " + String.join(", ", FileValidator.DOCUMENT_EXT.stream().sorted().toList()));
        }
        a.setTitle(r.title().trim());
        a.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        a.setInstructions(r.instructions() == null || r.instructions().isBlank() ? null : r.instructions().trim());
        a.setDueAt(r.dueAt());
        a.setMaxMarks(r.maxMarks());
        a.setAllowedTypes(String.join(",", clean));
        a.setUpdatedAt(Instant.now());
    }

    // ---------- teacher ----------
    @Transactional
    public AssignmentDto create(Long subjectId, AssignmentRequest r) {
        Subject s = access.requireOwnedSubject(subjectId);
        Assignment a = new Assignment();
        a.setSubjectId(subjectId);
        apply(a, r);
        a = assignments.save(a);
        for (Enrollment e : enrollments.findBySubjectIdAndStatus(subjectId, "ACTIVE")) {
            notifications.notify(e.getStudentId(), "NEW_ASSIGNMENT", "New assignment: " + a.getTitle(), s.getTitle(),
                    "/student/subjects/" + subjectId + "?tab=assignments", "assignment-" + a.getId());
        }
        return dto(a, null, 0L, 0L);
    }

    @Transactional
    public AssignmentDto update(Long id, AssignmentRequest r) {
        Assignment a = ownedAssignment(id);
        apply(a, r);
        assignments.save(a);
        return dto(a, null, (long) submissions.findByAssignmentId(id).size(), null);
    }

    @Transactional
    public void delete(Long id) {
        Assignment a = ownedAssignment(id);
        List<AssignmentSubmission> subs = submissions.findByAssignmentId(id);
        Long att = a.getAttachmentResourceId();
        assignments.delete(a);
        assignments.flush();
        subs.forEach(s -> dropResource(s.getResourceId()));
        dropResource(att);
    }

    private void dropResource(Long id) {
        if (id == null) return;
        resources.findById(id).ifPresent(r -> { resources.delete(r); storage.delete(r.getStoredName()); });
    }

    @Transactional
    public AssignmentDto attach(Long id, MultipartFile file) {
        Assignment a = ownedAssignment(id);
        LearningResource r = content.storeResource(a.getSubjectId(), file, FileValidator.DOCUMENT_EXT, "ATTACHMENT");
        Long old = a.getAttachmentResourceId();
        a.setAttachmentResourceId(r.getId());
        assignments.save(a);
        assignments.flush();
        dropResource(old);
        return dto(a, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> teacherList(Long subjectId) {
        access.requireOwnedSubject(subjectId);
        List<Assignment> list = assignments.findBySubjectIdOrderByDueAtAsc(subjectId);
        Map<Long, List<AssignmentSubmission>> subs = list.isEmpty() ? Map.of() : submissions.findByAssignmentIdIn(list.stream().map(Assignment::getId).toList())
                .stream().collect(Collectors.groupingBy(AssignmentSubmission::getAssignmentId));
        return list.stream().map(a -> {
            List<AssignmentSubmission> s = subs.getOrDefault(a.getId(), List.of());
            return dto(a, null, (long) s.size(), s.stream().filter(x -> x.getMarks() == null).count());
        }).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDto teacherGet(Long id) {
        Assignment a = ownedAssignment(id);
        List<AssignmentSubmission> s = submissions.findByAssignmentId(id);
        return dto(a, null, (long) s.size(), s.stream().filter(x -> x.getMarks() == null).count());
    }

    @Transactional(readOnly = true)
    public List<SubmissionRow> submissionRows(Long assignmentId) {
        Assignment a = ownedAssignment(assignmentId);
        Map<Long, AssignmentSubmission> byStudent = submissions.findByAssignmentId(assignmentId).stream()
                .collect(Collectors.toMap(AssignmentSubmission::getStudentId, s -> s));
        Set<Long> ids = new LinkedHashSet<>();
        enrollments.findBySubjectIdAndStatus(a.getSubjectId(), "ACTIVE").forEach(e -> ids.add(e.getStudentId()));
        ids.addAll(byStudent.keySet());
        Map<Long, User> names = users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
        boolean pastDue = Instant.now().isAfter(a.getDueAt());
        return ids.stream().map(sid -> {
            AssignmentSubmission s = byStudent.get(sid);
            User u = names.get(sid);
            String status = s == null ? (pastDue ? "MISSING" : "NOT_SUBMITTED") : s.getMarks() != null ? "GRADED" : s.isLate() ? "LATE" : "SUBMITTED";
            return new SubmissionRow(sid, u == null ? "Student" : u.getName(), u == null ? "" : u.getEmail(), status, sub(s));
        }).sorted(Comparator.comparing(SubmissionRow::studentName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional
    public SubmissionDto grade(Long submissionId, GradeSubmissionRequest r) {
        AssignmentSubmission s = submissions.findById(submissionId).orElseThrow(() -> ApiException.notFound("Submission not found"));
        Assignment a = ownedAssignment(s.getAssignmentId());
        if (r.marks() > a.getMaxMarks()) throw ApiException.badRequest("Marks cannot exceed " + a.getMaxMarks());
        s.setMarks(r.marks());
        s.setFeedback(r.feedback() == null || r.feedback().isBlank() ? null : r.feedback().trim());
        s.setGradedAt(Instant.now());
        submissions.save(s);
        notifications.notify(s.getStudentId(), "ASSIGNMENT_GRADED", "Assignment graded: " + a.getTitle(), "You scored " + r.marks() + " / " + a.getMaxMarks(),
                "/student/subjects/" + a.getSubjectId() + "?tab=assignments", null);
        return sub(s);
    }

    // ---------- student ----------
    @Transactional(readOnly = true)
    public List<AssignmentDto> studentList(Long subjectId) {
        access.requireEnrolledStudent(subjectId);
        Long sid = CurrentUser.id();
        List<Assignment> list = assignments.findBySubjectIdOrderByDueAtAsc(subjectId);
        Map<Long, AssignmentSubmission> mine = list.isEmpty() ? Map.of() : submissions.findByAssignmentIdIn(list.stream().map(Assignment::getId).toList()).stream()
                .filter(s -> s.getStudentId().equals(sid)).collect(Collectors.toMap(AssignmentSubmission::getAssignmentId, s -> s));
        return list.stream().map(a -> dto(a, mine.get(a.getId()), null, null)).toList();
    }

    @Transactional
    public SubmissionDto submit(Long assignmentId, MultipartFile file) {
        Assignment a = assignments.findById(assignmentId).orElseThrow(() -> ApiException.notFound("Assignment not found"));
        Subject subject = access.requireEnrolledStudent(a.getSubjectId());
        Instant now = Instant.now();
        if (now.isAfter(a.getDueAt())) throw ApiException.badRequest("The deadline for this assignment has passed");
        Set<String> allowed = new HashSet<>(types(a));
        LearningResource r = content.storeResource(a.getSubjectId(), file, allowed, "SUBMISSION");
        Long sid = CurrentUser.id();
        AssignmentSubmission s = submissions.findByAssignmentIdAndStudentId(assignmentId, sid).orElseGet(() -> {
            AssignmentSubmission n = new AssignmentSubmission();
            n.setAssignmentId(assignmentId);
            n.setStudentId(sid);
            return n;
        });
        Long old = s.getResourceId();
        s.setResourceId(r.getId());
        s.setSubmittedAt(now);
        s.setLate(false);
        s.setMarks(null);
        s.setFeedback(null);
        s.setGradedAt(null);
        submissions.save(s);
        submissions.flush();
        dropResource(old);
        notifications.notify(subject.getTeacherId(), "ASSIGNMENT_SUBMITTED", "New submission: " + a.getTitle(), CurrentUser.get().getName() + " submitted work",
                "/teacher/assignments/" + assignmentId + "/submissions", null);
        return sub(s);
    }
}
