package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Comment response")
public record CommentResponse(
        Long id,
        String text,
        Long taskId,
        Long authorId,
        String authorUsername,
        String authorFullName,
        boolean edited,
        Instant createdAt,
        Instant updatedAt) {

}
