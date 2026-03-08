package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record TaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String assignee,
    @NotBlank String creator,
    @NotNull Priority priority,
    Instant dueDate
) {
}
