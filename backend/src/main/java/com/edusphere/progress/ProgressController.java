package com.edusphere.progress;

import com.edusphere.common.CurrentUser;
import com.edusphere.subject.AccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@Tag(name = "Progress")
@Validated
public class ProgressController {
    public record PositionRequest(int position) {}
    public record NotesRequest(@Size(max = 10000, message = "Notes are too long") String notes) {}

    private final ProgressService service;
    private final AccessService access;

    public ProgressController(ProgressService service, AccessService access) { this.service = service; this.access = access; }

    @GetMapping("/subjects/{subjectId}/progress")
    @Operation(summary = "Progress of the signed-in student in one subject")
    public ProgressService.SubjectProgress subject(@PathVariable Long subjectId) {
        access.requireEnrolledStudent(subjectId);
        return service.forSubject(subjectId, CurrentUser.id());
    }

    @PutMapping("/lessons/{lessonId}/position")
    @Operation(summary = "Save playback position (clamped by the server)")
    public ProgressService.LessonProgressDto position(@PathVariable Long lessonId, @RequestBody PositionRequest req) {
        return service.savePosition(lessonId, CurrentUser.id(), req.position());
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @Operation(summary = "Mark a lesson complete")
    public ProgressService.LessonProgressDto complete(@PathVariable Long lessonId) {
        return service.setCompleted(lessonId, CurrentUser.id(), true);
    }

    @DeleteMapping("/lessons/{lessonId}/complete")
    @Operation(summary = "Mark a lesson not complete")
    public ProgressService.LessonProgressDto uncomplete(@PathVariable Long lessonId) {
        return service.setCompleted(lessonId, CurrentUser.id(), false);
    }

    @PutMapping("/lessons/{lessonId}/notes")
    @Operation(summary = "Save personal notes for a lesson")
    public ProgressService.LessonProgressDto notes(@PathVariable Long lessonId, @Valid @RequestBody NotesRequest req) {
        return service.saveNotes(lessonId, CurrentUser.id(), req.notes());
    }
}
