package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for adding a member to a project")
public record ProjectMemberRequest(
        @Schema(description = "User identifier", example = "7")
                @NotNull
                @Positive
                Long userId,
        @Schema(description = "Project role", example = "MEMBER")
                ProjectRole role) {
}
