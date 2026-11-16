package com.edusphere.assignment;

import com.edusphere.assignment.AssignmentDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Assignments")
public class AssignmentController {
    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @PostMapping("/teacher/subjects/{subjectId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an assignment")
    public AssignmentDto create(@PathVariable Long subjectId, @Valid @RequestBody AssignmentRequest req) { return service.create(subjectId, req); }

    @GetMapping("/teacher/subjects/{subjectId}/assignments")
    @Operation(summary = "Assignments of my subject")
    public List<AssignmentDto> list(@PathVariable Long subjectId) { return service.teacherList(subjectId); }

    @GetMapping("/teacher/assignments/{id}")
    @Operation(summary = "One assignment")
    public AssignmentDto get(@PathVariable Long id) { return service.teacherGet(id); }

    @PutMapping("/teacher/assignments/{id}")
    @Operation(summary = "Update an assignment")
    public AssignmentDto update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest req) { return service.update(id, req); }

    @DeleteMapping("/teacher/assignments/{id}")
    @Operation(summary = "Delete an assignment and its submissions")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("message", "Assignment deleted");
    }

    @PostMapping(value = "/teacher/assignments/{id}/attachment", consumes = "multipart/form-data")
    @Operation(summary = "Attach a file to an assignment")
    public AssignmentDto attach(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return service.attach(id, file); }

    @GetMapping("/teacher/assignments/{id}/submissions")
    @Operation(summary = "Submissions, including missing work")
    public List<SubmissionRow> submissions(@PathVariable Long id) { return service.submissionRows(id); }

    @PostMapping("/teacher/submissions/{id}/grade")
    @Operation(summary = "Award marks and feedback")
    public SubmissionDto grade(@PathVariable Long id, @Valid @RequestBody GradeSubmissionRequest req) { return service.grade(id, req); }

    @GetMapping("/student/subjects/{subjectId}/assignments")
    @Operation(summary = "Assignments with my submission status")
    public List<AssignmentDto> studentList(@PathVariable Long subjectId) { return service.studentList(subjectId); }

    @PostMapping(value = "/student/assignments/{id}/submit", consumes = "multipart/form-data")
    @Operation(summary = "Submit (or re-submit until the due date) one file")
    public SubmissionDto submit(@PathVariable Long id, @RequestParam("file") MultipartFile file) { return service.submit(id, file); }
}
