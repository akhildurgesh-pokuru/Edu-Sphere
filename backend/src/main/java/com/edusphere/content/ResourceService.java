package com.edusphere.content;

import com.edusphere.common.ApiException;
import com.edusphere.common.CurrentUser;
import com.edusphere.domain.LearningResource;
import com.edusphere.repository.AssignmentSubmissionRepository;
import com.edusphere.repository.LearningResourceRepository;
import com.edusphere.repository.LessonRepository;
import com.edusphere.security.AppUserDetails;
import com.edusphere.storage.StorageService;
import com.edusphere.subject.AccessService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {
    public record Download(Resource resource, String name, String contentType, boolean inline) {}

    private final LearningResourceRepository resources;
    private final LessonRepository lessons;
    private final AssignmentSubmissionRepository submissions;
    private final com.edusphere.repository.AssignmentRepository assignments;
    private final AccessService access;
    private final StorageService storage;

    public ResourceService(LearningResourceRepository resources, LessonRepository lessons, AssignmentSubmissionRepository submissions,
                           com.edusphere.repository.AssignmentRepository assignments, AccessService access, StorageService storage) {
        this.resources = resources; this.lessons = lessons; this.submissions = submissions; this.assignments = assignments;
        this.access = access; this.storage = storage;
    }

    /** Only the owning teacher or an actively enrolled student may read a file; everyone else gets 404. */
    @Transactional(readOnly = true)
    public Download open(Long id) {
        AppUserDetails u = CurrentUser.get();
        LearningResource r = resources.findById(id).orElseThrow(() -> ApiException.notFound("File not found"));
        AccessService.Access a;
        try {
            a = access.requireAccess(r.getSubjectId());
        } catch (ApiException e) {
            throw ApiException.notFound("File not found");
        }
        if (!a.owner()) {
            boolean allowed = switch (r.getKind()) {
                case "SUBMISSION" -> submissions.existsByResourceIdAndStudentId(id, u.getId());
                case "ATTACHMENT" -> assignments.existsByAttachmentResourceId(id);
                default -> {
                    var ls = lessons.findByResourceId(id);
                    yield !ls.isEmpty() && ls.stream().anyMatch(l -> l.isPublished());
                }
            };
            if (!allowed) throw ApiException.notFound("File not found");
        }
        boolean inline = switch (r.getKind()) { case "VIDEO", "PDF", "IMAGE" -> true; default -> false; };
        return new Download(storage.load(r.getStoredName()), r.getOriginalName(), r.getContentType(), inline);
    }
}
