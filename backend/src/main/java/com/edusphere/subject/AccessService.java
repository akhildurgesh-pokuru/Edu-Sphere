package com.edusphere.subject;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.domain.Subject;
import com.edusphere.repository.EnrollmentRepository;
import com.edusphere.repository.SubjectRepository;
import com.edusphere.security.AppUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shared authorization helper: "is the caller the owner or an ACTIVE enrolled student of this subject?".
 * Every lesson, resource, quiz, assignment and discussion endpoint goes through here.
 */
@Service
public class AccessService {
    public record Access(Subject subject, boolean owner) {}

    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;

    public AccessService(SubjectRepository subjects, EnrollmentRepository enrollments) {
        this.subjects = subjects; this.enrollments = enrollments;
    }

    @Transactional(readOnly = true)
    public Subject requireOwnedSubject(Long subjectId) {
        return subjects.findByIdAndTeacherId(subjectId, CurrentUser.id())
                .orElseThrow(() -> ApiException.notFound("Subject not found"));
    }

    /** Owner teacher, or student with an ACTIVE enrollment in a non-draft subject. */
    @Transactional(readOnly = true)
    public Access requireAccess(Long subjectId) {
        AppUserDetails u = CurrentUser.get();
        Subject s = subjects.findById(subjectId).orElseThrow(() -> ApiException.notFound("Subject not found"));
        if (s.getTeacherId().equals(u.getId())) return new Access(s, true);
        if (u.isTeacher()) throw ApiException.notFound("Subject not found");
        boolean enrolled = enrollments.existsBySubjectIdAndStudentIdAndStatus(subjectId, u.getId(), "ACTIVE");
        if (enrolled && !"DRAFT".equals(s.getStatus())) return new Access(s, false);
        if ("PUBLISHED".equals(s.getStatus())) throw ApiException.forbidden("Enroll in this course to access its content");
        throw ApiException.notFound("Subject not found");
    }

    /** Same as requireAccess, but only an enrolled student passes (used for taking quizzes, submitting work). */
    public Subject requireEnrolledStudent(Long subjectId) {
        Access a = requireAccess(subjectId);
        if (a.owner()) throw ApiException.forbidden("Only enrolled students can do that");
        return a.subject();
    }

    public boolean isActiveEnrolled(Long subjectId, Long studentId) {
        return enrollments.existsBySubjectIdAndStudentIdAndStatus(subjectId, studentId, "ACTIVE");
    }
}
