package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

@Schema(description = "Payload for partial task update")
public record TaskPatchRequest(
        @Schema(description = "Task title", example = "Prepare release checklist")
                @Pattern(regexp = "^\\s*\\S.*$", message = "title must not be blank")
                String title,
        @Schema(description = "Task description", example = "Collect and verify the release checklist.")
                @Pattern(regexp = "^\\s*\\S.*$", message = "description must not be blank")
                String description,
        @Schema(description = "Project identifier", example = "4")
                @Positive
                Long projectId,
        @Schema(description = "Assignee identifier", example = "9")
                @Positive
                Long assigneeId,
        @Schema(description = "Task status", example = "IN_PROGRESS")
                Status status,
        @Schema(description = "Task priority", example = "HIGH")
                Priority priority,
        @Schema(description = "Task due date", example = "2026-03-25T18:00:00Z")
                Instant dueDate) {

}
