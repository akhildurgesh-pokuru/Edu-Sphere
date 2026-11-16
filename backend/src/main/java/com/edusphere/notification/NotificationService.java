package com.edusphere.notification;

import com.edusphere.common.ApiException;
import com.edusphere.common.PageResponse;
import com.edusphere.domain.Enrollment;
import com.edusphere.domain.Notification;
import com.edusphere.repository.EnrollmentRepository;
import com.edusphere.mail.EmailService;
import com.edusphere.repository.NotificationRepository;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public record NotificationDto(Long id, String type, String title, String message, String link, boolean read, Instant createdAt) {}

    private final NotificationRepository repo;
    private final EnrollmentRepository enrollments;
    private final EmailService emails;

    public NotificationService(NotificationRepository repo, EnrollmentRepository enrollments, EmailService emails) {
        this.repo = repo; this.enrollments = enrollments; this.emails = emails;
    }

    /** Creates one notification. When refKey is set it is created at most once per user. */
    @Transactional
    public void notify(Long userId, String type, String title, String message, String link, String refKey) {
        if (refKey != null && repo.existsByUserIdAndRefKey(userId, refKey)) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message == null ? null : message.substring(0, Math.min(message.length(), 500)));
        n.setLink(link);
        n.setRefKey(refKey);
        repo.save(n);
        emails.sendNotification(userId, title, n.getMessage(), link);
    }

    @Transactional
    public void notifyEnrolled(Long subjectId, Long exceptUserId, String type, String title, String message, String link, String refKeyPrefix) {
        for (Enrollment e : enrollments.findBySubjectIdAndStatus(subjectId, "ACTIVE")) {
            if (e.getStudentId().equals(exceptUserId)) continue;
            notify(e.getStudentId(), type, title, message, link, refKeyPrefix == null ? null : refKeyPrefix);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, int page, int size) {
        var p = repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PageResponse.of(p, l -> l.stream().map(NotificationService::dto).toList());
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) { return repo.countByUserIdAndReadFalse(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repo.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Notification not found"));
        n.setRead(true);
        repo.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) { repo.markAllRead(userId); }

    static NotificationDto dto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
