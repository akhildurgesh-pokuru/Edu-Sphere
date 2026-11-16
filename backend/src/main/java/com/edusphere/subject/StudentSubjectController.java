package com.edusphere.subject;

import com.edusphere.common.PageResponse;
import com.edusphere.subject.SubjectDtos.SubjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@Tag(name = "Student catalog")
public class StudentSubjectController {
    private final SubjectService service;

    public StudentSubjectController(SubjectService service) { this.service = service; }

    @GetMapping("/catalog")
    @Operation(summary = "Published subjects from every teacher, 12 per page")
    public PageResponse<SubjectResponse> catalog(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
                                                 @RequestParam(defaultValue = "0") int page) {
        return service.catalog(q, category, page);
    }

    @GetMapping("/categories")
    @Operation(summary = "Categories used by published subjects")
    public List<String> categories() { return service.categories(); }

    @GetMapping("/subjects")
    @Operation(summary = "Subjects I am enrolled in")
    public List<SubjectResponse> mine() { return service.enrolledSubjects(); }

    @GetMapping("/subjects/{id}")
    @Operation(summary = "Course detail (published, or enrolled)")
    public SubjectResponse detail(@PathVariable Long id) { return service.studentDetail(id); }

    @PostMapping("/subjects/{id}/enroll")
    @Operation(summary = "Enroll in a published subject")
    public SubjectResponse enroll(@PathVariable Long id) { return service.enroll(id); }

    @PostMapping("/subjects/{id}/leave")
    @Operation(summary = "Leave a subject")
    public Map<String, String> leave(@PathVariable Long id) {
        service.leave(id);
        return Map.of("message", "You left the course");
    }
}
