package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull Long projectId,
    Long creatorId,
    Long assigneeId,
    Status status,
    @NotNull Priority priority,
    Instant dueDate
) {
}
