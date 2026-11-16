package com.edusphere.content;

import com.edusphere.content.ContentDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@Tag(name = "Lessons and files")
public class LessonController {
    private final ContentService content;
    private final ResourceService resources;

    public LessonController(ContentService content, ResourceService resources) { this.content = content; this.resources = resources; }

    @GetMapping("/student/subjects/{subjectId}/curriculum")
    @Operation(summary = "Curriculum outline; content is locked until enrolled")
    public CurriculumResponse curriculum(@PathVariable Long subjectId) { return content.studentCurriculum(subjectId); }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Lesson content (owner teacher or actively enrolled student)")
    public LessonDetail lesson(@PathVariable Long lessonId) { return content.lesson(lessonId); }

    @GetMapping("/resources/{id}")
    @Operation(summary = "Download or stream a file (supports HTTP Range)")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download) {
        ResourceService.Download d = resources.open(id);
        boolean inline = d.inline() && !download;
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(d.name(), StandardCharsets.UTF_8).build();
        var b = ResponseEntity.ok();
        if (!inline) b.header("Content-Security-Policy", "sandbox; default-src 'none'");
        return b.contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("X-Content-Type-Options", "nosniff")
                .body(d.resource());
    }
}
