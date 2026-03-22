package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Detailed task response")
public record TaskDetailsResponse(
        Long id,
        String title,
        String description,
        Status status,
        Priority priority,
        Long projectId,
        Long creatorId,
        String creatorUsername,
        Long assigneeId,
        String assigneeUsername,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt,
        List<TaskTagSummaryResponse> tags,
        List<TaskCommentSummaryResponse> comments) {

}
