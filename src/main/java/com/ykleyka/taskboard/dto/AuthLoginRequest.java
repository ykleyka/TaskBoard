package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for signing in")
public record AuthLoginRequest(
        @Schema(description = "Username or email", example = "jdoe")
                @NotBlank
                String login,
        @Schema(
                        description = "User password",
                        example = "StrongPass123!",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                String password) {
}
