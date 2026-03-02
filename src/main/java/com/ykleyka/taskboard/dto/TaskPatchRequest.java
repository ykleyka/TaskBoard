package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.Status;

public record TaskPatchRequest(
    String title,
    String description,
    Status status,
    String assignee
) {
}
