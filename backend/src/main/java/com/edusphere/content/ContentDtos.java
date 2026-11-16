package com.edusphere.content;

import com.edusphere.progress.ProgressService.LessonProgressDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ContentDtos {
    private ContentDtos() {}

    public record ModuleRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 2000, message = "Description is too long") String description) {}

    public record LessonRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @NotBlank(message = "Type is required") String type,
            @Size(max = 100000, message = "Text is too long") String contentText,
            @Size(max = 500, message = "Link is too long") String externalUrl,
            Long resourceId,
            Integer durationSeconds,
            Boolean published) {}

    public record ReorderRequest(@NotEmpty(message = "ids are required") List<Long> ids) {}

    public record ResourceDto(Long id, String name, String kind, String contentType, long sizeBytes, String url) {}

    public record CurriculumLesson(Long id, String title, String type, int durationSeconds, int position, boolean published,
                                   boolean completed, boolean hasResource) {}

    public record CurriculumModule(Long id, String title, String description, int position, List<CurriculumLesson> lessons) {}

    public record CurriculumResponse(Long subjectId, boolean locked, List<CurriculumModule> modules) {}

    public record LessonDetail(Long id, Long subjectId, Long moduleId, String title, String type, String contentText, String externalUrl,
                               ResourceDto resource, int durationSeconds, boolean published, LessonProgressDto progress) {}
}
