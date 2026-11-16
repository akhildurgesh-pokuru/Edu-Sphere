package com.edusphere.content;

import com.edusphere.content.ContentDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teacher")
@Tag(name = "Teacher curriculum")
public class TeacherContentController {
    private final ContentService service;

    public TeacherContentController(ContentService service) { this.service = service; }

    @GetMapping("/subjects/{subjectId}/curriculum")
    @Operation(summary = "Modules and lessons of my subject (including drafts)")
    public CurriculumResponse curriculum(@PathVariable Long subjectId) { return service.teacherCurriculum(subjectId); }

    @PostMapping("/subjects/{subjectId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a module")
    public CurriculumModule createModule(@PathVariable Long subjectId, @Valid @RequestBody ModuleRequest req) {
        return service.createModule(subjectId, req);
    }

    @PutMapping("/modules/{moduleId}")
    @Operation(summary = "Edit a module")
    public CurriculumModule updateModule(@PathVariable Long moduleId, @Valid @RequestBody ModuleRequest req) {
        return service.updateModule(moduleId, req);
    }

    @DeleteMapping("/modules/{moduleId}")
    @Operation(summary = "Delete a module and its lessons")
    public Map<String, String> deleteModule(@PathVariable Long moduleId) {
        service.deleteModule(moduleId);
        return Map.of("message", "Module deleted");
    }

    @PostMapping("/subjects/{subjectId}/modules/reorder")
    @Operation(summary = "Reorder modules (full ordered id list)")
    public Map<String, String> reorderModules(@PathVariable Long subjectId, @Valid @RequestBody ReorderRequest req) {
        service.reorderModules(subjectId, req.ids());
        return Map.of("message", "Reordered");
    }

    @PostMapping("/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a lesson")
    public LessonDetail createLesson(@PathVariable Long moduleId, @Valid @RequestBody LessonRequest req) {
        return service.createLesson(moduleId, req);
    }

    @PutMapping("/lessons/{lessonId}")
    @Operation(summary = "Edit a lesson")
    public LessonDetail updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequest req) {
        return service.updateLesson(lessonId, req);
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Operation(summary = "Delete a lesson")
    public Map<String, String> deleteLesson(@PathVariable Long lessonId) {
        service.deleteLesson(lessonId);
        return Map.of("message", "Lesson deleted");
    }

    @PostMapping("/modules/{moduleId}/lessons/reorder")
    @Operation(summary = "Reorder lessons in a module")
    public Map<String, String> reorderLessons(@PathVariable Long moduleId, @Valid @RequestBody ReorderRequest req) {
        service.reorderLessons(moduleId, req.ids());
        return Map.of("message", "Reordered");
    }

    @PostMapping(value = "/subjects/{subjectId}/resources", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a video, PDF, PPT/PPTX or document")
    public ResourceDto upload(@PathVariable Long subjectId, @RequestParam("file") MultipartFile file) {
        return service.upload(subjectId, file);
    }
}
