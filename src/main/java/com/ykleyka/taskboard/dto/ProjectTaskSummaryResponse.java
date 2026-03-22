package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Short project task representation")
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
