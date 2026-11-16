package com.edusphere.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SubjectDtos {
    private SubjectDtos() {}

    public record SubjectRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 5000, message = "Description is too long") String description,
            @Size(max = 80, message = "Category is too long") String category) {}

    public record StatusRequest(@NotBlank(message = "Status is required") String status) {}

    public record SubjectResponse(Long id, String title, String description, String category, String status,
                                  Long teacherId, String teacherName, long enrollmentCount, long lessonCount,
                                  Boolean enrolled, boolean hasCover, Double progressPercent,
                                  Instant createdAt, Instant updatedAt, Instant publishedAt) {}

    public record StudentRow(Long studentId, String name, String email, Instant enrolledAt, double progressPercent) {}
}
