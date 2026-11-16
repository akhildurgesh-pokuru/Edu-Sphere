package com.edusphere.subject;

import com.edusphere.subject.SubjectDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teacher/subjects")
@Tag(name = "Teacher subjects")
public class TeacherSubjectController {
    private final SubjectService service;

    public TeacherSubjectController(SubjectService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a subject (draft)")
    public SubjectResponse create(@Valid @RequestBody SubjectRequest req) { return service.create(req); }

    @GetMapping
    @Operation(summary = "List my subjects")
    public List<SubjectResponse> list() { return service.listOwn(); }

    @GetMapping("/{id}")
    @Operation(summary = "Read one of my subjects")
    public SubjectResponse get(@PathVariable Long id) { return service.getOwn(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of my subjects")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest req) { return service.update(id, req); }

    @PostMapping("/{id}/status")
    @Operation(summary = "Set DRAFT, PUBLISHED or ARCHIVED")
    public SubjectResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest req) { return service.setStatus(id, req.status()); }

    @PostMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @Operation(summary = "Upload a cover image")
    public SubjectResponse cover(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return service.setCover(id, file); }

    @GetMapping("/{id}/students")
    @Operation(summary = "Students enrolled in my subject")
    public List<StudentRow> students(@PathVariable Long id) { return service.students(id); }

    @DeleteMapping("/{id}/students/{studentId}")
    @Operation(summary = "Remove a student (they cannot re-enroll)")
    public Map<String, String> remove(@PathVariable Long id, @PathVariable Long studentId) {
        service.removeStudent(id, studentId);
        return Map.of("message", "Student removed");
    }
}
