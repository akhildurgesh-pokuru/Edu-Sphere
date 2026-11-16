package com.edusphere.assignment;

import com.edusphere.content.ContentDtos.ResourceDto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class AssignmentDtos {
    private AssignmentDtos() {}

    public record AssignmentRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @Size(max = 5000) String description,
            @Size(max = 10000) String instructions,
            @NotNull(message = "Due date is required") Instant dueAt,
            @NotNull(message = "Max marks is required") @DecimalMin(value = "1", message = "At least 1") @DecimalMax(value = "1000", message = "At most 1000") Double maxMarks,
            List<String> allowedTypes) {}

    public record SubmissionDto(Long id, ResourceDto file, Instant submittedAt, boolean late, Double marks, String feedback, Instant gradedAt) {}

    public record AssignmentDto(Long id, Long subjectId, String title, String description, String instructions, Instant dueAt, double maxMarks,
                                List<String> allowedTypes, ResourceDto attachment, Instant createdAt, boolean pastDue,
                                SubmissionDto mySubmission, Long submissionCount, Long pendingGrading) {}

    public record SubmissionRow(Long studentId, String studentName, String studentEmail, String status, SubmissionDto submission) {}

    public record GradeSubmissionRequest(
            @NotNull(message = "Marks are required") @DecimalMin(value = "0", message = "Marks cannot be negative") Double marks,
            @Size(max = 5000) String feedback) {}
}
