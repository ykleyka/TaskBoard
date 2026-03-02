package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.Status;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    String title,
    String description,
    Status status,
    String assignee,
    String creator,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
