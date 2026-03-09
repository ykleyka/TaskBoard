package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;

public record TaskPatchRequest(
    String title,
    String description,
    Long projectId,
    Long assigneeId,
    Status status,
    Priority priority,
    Instant dueDate
) {
}
