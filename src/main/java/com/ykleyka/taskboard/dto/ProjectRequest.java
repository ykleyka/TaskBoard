package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for creating or fully updating a project")
public record ProjectRequest(
        @Schema(description = "Project name", example = "Platform Redesign")
                @NotBlank
                String name,
        @Schema(description = "Project description", example = "Migration to the new design system.")
                String description,
        @Schema(description = "Legacy owner identifier. Authenticated clients can omit it.", example = "1")
                @Positive
                Long ownerId) {

}
