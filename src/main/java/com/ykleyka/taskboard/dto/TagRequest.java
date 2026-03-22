package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for creating a tag")
public record TagRequest(
        @Schema(description = "Tag name", example = "backend")
                @NotBlank
                String name) {

}
