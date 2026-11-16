package com.edusphere.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class CommunityDtos {
    private CommunityDtos() {}

    public record ThreadRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @NotBlank(message = "Message is required") @Size(max = 10000, message = "Message is too long") String body) {}

    public record ReplyRequest(@NotBlank(message = "Reply cannot be empty") @Size(max = 10000, message = "Reply is too long") String body) {}

    public record FlagRequest(boolean value) {}

    public record ThreadSummary(Long id, String title, String preview, Long authorId, String authorName, String authorRole, long replyCount,
                                boolean pinned, boolean closed, boolean unread, Instant createdAt, Instant lastActivityAt) {}

    public record ReplyDto(Long id, Long authorId, String authorName, String authorRole, String body, Instant createdAt, boolean canDelete) {}

    public record ThreadDetail(Long id, Long subjectId, String title, String body, Long authorId, String authorName, String authorRole,
                               boolean pinned, boolean closed, Instant createdAt, boolean canDelete, boolean canModerate, List<ReplyDto> replies) {}

    public record AnnouncementRequest(
            @NotBlank(message = "Title is required") @Size(max = 200, message = "Title is too long") String title,
            @NotBlank(message = "Message is required") @Size(max = 10000, message = "Message is too long") String body,
            Boolean pinned) {}

    public record AnnouncementDto(Long id, Long subjectId, String subjectTitle, String title, String body, boolean pinned, String authorName,
                                  Instant createdAt, Instant updatedAt) {}
}
