package com.ykleyka.taskboard.dto;

import java.time.Instant;

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
