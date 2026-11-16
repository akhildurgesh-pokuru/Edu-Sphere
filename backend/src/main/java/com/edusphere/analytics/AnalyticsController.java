package com.edusphere.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) { this.service = service; }

    @GetMapping("/teacher/dashboard")
    @Operation(summary = "Teacher dashboard numbers, charts and activity")
    public Map<String, Object> teacherDashboard() { return service.teacherDashboard(); }

    @GetMapping("/teacher/subjects/{id}/analytics")
    @Operation(summary = "Analytics for one of my subjects")
    public Map<String, Object> subject(@PathVariable Long id) { return service.subjectAnalytics(id); }

    @GetMapping("/student/dashboard")
    @Operation(summary = "Student dashboard")
    public Map<String, Object> studentDashboard() { return service.studentDashboard(); }

    @GetMapping("/student/progress")
    @Operation(summary = "Overall and per-subject progress, quiz scores, assignment marks, deadlines")
    public Map<String, Object> studentProgress() { return service.studentProgress(); }
}
