package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

/** Request for task creation. */
public record TaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String assignee
) {
}
