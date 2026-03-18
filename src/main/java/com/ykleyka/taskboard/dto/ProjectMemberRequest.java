package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequest(
        @NotNull Long userId,
        ProjectRole role
) {
}
