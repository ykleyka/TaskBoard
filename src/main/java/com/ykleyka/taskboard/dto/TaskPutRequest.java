package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request for full task replacement. */
public record TaskPutRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotNull Status status,
    @NotBlank String assignee
) {
}
