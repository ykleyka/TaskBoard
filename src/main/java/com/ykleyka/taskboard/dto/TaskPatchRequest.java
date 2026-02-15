package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.Status;

/** Request for partial task update. */
public record TaskPatchRequest(
    String title,
    String description,
    Status status,
    String assignee
) {
}
