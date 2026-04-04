package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for updating a project member role")
public record ProjectMemberRoleRequest(
        @Schema(description = "Project role", example = "MANAGER")
                @NotNull
                ProjectRole role) {
}
