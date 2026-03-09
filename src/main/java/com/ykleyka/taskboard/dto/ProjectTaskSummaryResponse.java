package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;

public record ProjectTaskSummaryResponse(
        Long id,
        String title,
        Status status,
        Priority priority,
        Long creatorId,
        String creatorUsername,
        Long assigneeId,
        String assigneeUsername,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt) {

}
