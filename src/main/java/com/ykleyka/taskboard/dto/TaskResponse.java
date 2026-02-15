package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.Status;
import java.time.LocalDateTime;

/** Response payload representing a task. */
public record TaskResponse(
    Long id,
    String title,
    String description,
    Status status,
    String assignee,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
