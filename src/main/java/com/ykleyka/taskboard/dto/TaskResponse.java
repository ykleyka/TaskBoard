package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;

public record TaskResponse(
    Long id,
    String title,
    String description,
    Status status,
    Priority priority,
    String assignee,
    String creator,
    Instant dueDate,
    Instant createdAt,
    Instant updatedAt
) {
}
