package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Short task comment representation")
public record TaskCommentSummaryResponse(
        Long id,
        String text,
        Long authorId,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt) {

}
