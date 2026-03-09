package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public record TaskPatchRequest(
        @Pattern(regexp = "^\\s*\\S.*$", message = "title must not be blank") String title,
        @Pattern(regexp = "^\\s*\\S.*$", message = "description must not be blank")
                String description,
        Long projectId,
        Long assigneeId,
        Status status,
        Priority priority,
        Instant dueDate) {

}
