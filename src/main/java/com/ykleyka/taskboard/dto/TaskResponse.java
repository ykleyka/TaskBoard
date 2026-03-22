package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Task response")
public record TaskResponse(
        Long id,
        String title,
        String description,
        Status status,
        Priority priority,
        Long projectId,
        String projectName,
        Long creatorId,
        String creatorUsername,
        Long assigneeId,
        String assigneeUsername,
        Instant dueDate,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt) {
}
