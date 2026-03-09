package com.ykleyka.taskboard.dto;

import java.time.Instant;

public record TaskCommentSummaryResponse(
        Long id,
        String text,
        Long authorId,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt) {

}
