package com.edusphere.subject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
@Tag(name = "Subjects")
public class SubjectCoverController {
    private final SubjectService service;

    public SubjectCoverController(SubjectService service) { this.service = service; }

    @GetMapping("/{id}/cover")
    @Operation(summary = "Cover image of a subject the caller may see")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        var c = service.cover(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff").body(c.resource());
    }
}
