package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for creating or fully updating a project")
public record ProjectRequest(
        @Schema(description = "Project name", example = "Platform Redesign")
                @NotBlank
                String name,
        @Schema(description = "Project description", example = "Migration to the new design system.")
                String description,
        @Schema(description = "Project owner identifier", example = "1")
                @NotNull(groups = OnCreate.class)
                @Positive
                Long ownerId) {

}
