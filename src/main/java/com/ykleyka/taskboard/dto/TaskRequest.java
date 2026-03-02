package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String assignee,
    @NotBlank String creator
) {
}
