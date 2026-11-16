package com.edusphere.community;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.community.CommunityDtos.*;
import com.edusphere.domain.*;
import com.edusphere.notification.NotificationService;
import com.edusphere.repository.*;
import com.edusphere.subject.AccessService;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcements;
    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AccessService access;
    private final NotificationService notifications;

    public AnnouncementService(AnnouncementRepository announcements, SubjectRepository subjects, EnrollmentRepository enrollments,
                               UserRepository users, AccessService access, NotificationService notifications) {
        this.announcements = announcements; this.subjects = subjects; this.enrollments = enrollments; this.users = users;
        this.access = access; this.notifications = notifications;
    }

    private List<AnnouncementDto> toDtos(List<Announcement> list) {
        Map<Long, Subject> subs = subjects.findAllById(list.stream().map(Announcement::getSubjectId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Subject::getId, s -> s));
        Map<Long, User> authors = users.findAllById(list.stream().map(Announcement::getAuthorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        return list.stream().map(a -> new AnnouncementDto(a.getId(), a.getSubjectId(), subs.containsKey(a.getSubjectId()) ? subs.get(a.getSubjectId()).getTitle() : "",
                a.getTitle(), a.getBody(), a.isPinned(), authors.containsKey(a.getAuthorId()) ? authors.get(a.getAuthorId()).getName() : "Teacher",
                a.getCreatedAt(), a.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> list(Long subjectId) {
        access.requireAccess(subjectId);
        return toDtos(announcements.findBySubjectIdOrderByPinnedDescCreatedAtDesc(subjectId));
    }

    @Transactional
    public AnnouncementDto create(Long subjectId, AnnouncementRequest r) {
        Subject s = access.requireOwnedSubject(subjectId);
        Announcement a = new Announcement();
        a.setSubjectId(subjectId);
        a.setAuthorId(CurrentUser.id());
        a.setTitle(r.title().trim());
        a.setBody(r.body().trim());
        a.setPinned(Boolean.TRUE.equals(r.pinned()));
        a = announcements.save(a);
        for (Enrollment e : enrollments.findBySubjectIdAndStatus(subjectId, "ACTIVE")) {
            notifications.notify(e.getStudentId(), "ANNOUNCEMENT", s.getTitle() + ": " + a.getTitle(), a.getBody(),
                    "/student/subjects/" + subjectId + "?tab=announcements", "announcement-" + a.getId());
        }
        return toDtos(List.of(a)).get(0);
    }

    private Announcement owned(Long id) {
        Announcement a = announcements.findById(id).orElseThrow(() -> ApiException.notFound("Announcement not found"));
        access.requireOwnedSubject(a.getSubjectId());
        return a;
    }

    @Transactional
    public AnnouncementDto update(Long id, AnnouncementRequest r) {
        Announcement a = owned(id);
        a.setTitle(r.title().trim());
        a.setBody(r.body().trim());
        if (r.pinned() != null) a.setPinned(r.pinned());
        a.setUpdatedAt(Instant.now());
        return toDtos(List.of(announcements.save(a))).get(0);
    }

    @Transactional
    public void delete(Long id) { announcements.delete(owned(id)); }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> recentForStudent() {
        List<Long> ids = enrollments.findByStudentIdAndStatus(CurrentUser.id(), "ACTIVE").stream().map(Enrollment::getSubjectId).toList();
        if (ids.isEmpty()) return List.of();
        return toDtos(announcements.findTop10BySubjectIdInOrderByCreatedAtDesc(ids));
    }
}
