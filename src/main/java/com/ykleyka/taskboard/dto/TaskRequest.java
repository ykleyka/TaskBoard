package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

@Schema(description = "Payload for creating or fully updating a task")
public record TaskRequest(
        @Schema(description = "Task title", example = "Prepare release checklist")
                @NotBlank
                String title,
        @Schema(description = "Task description", example = "Collect and verify the release checklist.")
                @NotBlank
                String description,
        @Schema(description = "Project identifier", example = "4")
                @NotNull
                @Positive
                Long projectId,
        @Schema(description = "Legacy creator identifier. Authenticated clients can omit it.", example = "1")
                @Positive
                Long creatorId,
        @Schema(description = "Assignee identifier", example = "9")
                @Positive
                Long assigneeId,
        @Schema(description = "Task status", example = "IN_PROGRESS")
                Status status,
        @Schema(description = "Task priority", example = "HIGH")
                @NotNull
                Priority priority,
        @Schema(description = "Task due date", example = "2026-03-25T18:00:00Z")
                Instant dueDate) {
}
