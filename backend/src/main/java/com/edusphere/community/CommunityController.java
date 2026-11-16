package com.edusphere.community;

import com.edusphere.common.PageResponse;
import com.edusphere.community.CommunityDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Discussions and announcements")
public class CommunityController {
    private final DiscussionService discussions;
    private final AnnouncementService announcements;

    public CommunityController(DiscussionService discussions, AnnouncementService announcements) {
        this.discussions = discussions; this.announcements = announcements;
    }

    @GetMapping("/subjects/{subjectId}/discussions")
    @Operation(summary = "Threads of a subject (owner or enrolled student)")
    public PageResponse<ThreadSummary> threads(@PathVariable Long subjectId, @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page) {
        return discussions.list(subjectId, q, page);
    }

    @PostMapping("/subjects/{subjectId}/discussions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a thread")
    public ThreadDetail create(@PathVariable Long subjectId, @Valid @RequestBody ThreadRequest req) { return discussions.create(subjectId, req); }

    @GetMapping("/discussions/{id}")
    @Operation(summary = "Read a thread (marks it read)")
    public ThreadDetail get(@PathVariable Long id) { return discussions.get(id); }

    @PostMapping("/discussions/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reply to a thread")
    public ThreadDetail reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest req) { return discussions.reply(id, req); }

    @DeleteMapping("/discussions/{id}")
    @Operation(summary = "Delete a thread (author or owning teacher)")
    public Map<String, String> deleteThread(@PathVariable Long id) {
        discussions.deleteThread(id);
        return Map.of("message", "Deleted");
    }

    @DeleteMapping("/replies/{id}")
    @Operation(summary = "Delete a reply (author or owning teacher)")
    public Map<String, String> deleteReply(@PathVariable Long id) {
        discussions.deleteReply(id);
        return Map.of("message", "Deleted");
    }

    @PostMapping("/teacher/discussions/{id}/pin")
    @Operation(summary = "Pin or unpin a thread")
    public Map<String, String> pin(@PathVariable Long id, @RequestBody FlagRequest req) {
        discussions.setPinned(id, req.value());
        return Map.of("message", "ok");
    }

    @PostMapping("/teacher/discussions/{id}/close")
    @Operation(summary = "Close or reopen a thread")
    public Map<String, String> close(@PathVariable Long id, @RequestBody FlagRequest req) {
        discussions.setClosed(id, req.value());
        return Map.of("message", "ok");
    }

    @GetMapping("/subjects/{subjectId}/announcements")
    @Operation(summary = "Announcements of a subject")
    public List<AnnouncementDto> announcements(@PathVariable Long subjectId) { return announcements.list(subjectId); }

    @PostMapping("/teacher/subjects/{subjectId}/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post an announcement")
    public AnnouncementDto createAnnouncement(@PathVariable Long subjectId, @Valid @RequestBody AnnouncementRequest req) {
        return announcements.create(subjectId, req);
    }

    @PutMapping("/teacher/announcements/{id}")
    @Operation(summary = "Edit or pin an announcement")
    public AnnouncementDto updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest req) { return announcements.update(id, req); }

    @DeleteMapping("/teacher/announcements/{id}")
    @Operation(summary = "Delete an announcement")
    public Map<String, String> deleteAnnouncement(@PathVariable Long id) {
        announcements.delete(id);
        return Map.of("message", "Deleted");
    }

    @GetMapping("/student/announcements")
    @Operation(summary = "Recent announcements across my subjects")
    public List<AnnouncementDto> studentAnnouncements() { return announcements.recentForStudent(); }
}
