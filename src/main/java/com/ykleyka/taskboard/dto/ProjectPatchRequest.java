package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload for partial project update")
public record ProjectPatchRequest(
        @Schema(description = "Project name", example = "Platform Redesign")
                @Pattern(regexp = "^\\s*\\S.*$", message = "name must not be blank")
                String name,
        @Schema(description = "Project description", example = "Migration to the new design system.")
        String description) {

}
