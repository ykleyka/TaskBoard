package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TaskPutRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull Status status,
    @NotBlank String assignee,
    @NotNull Priority priority,
    Instant dueDate
) {
}
