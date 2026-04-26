package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for registering a user")
public record AuthRegisterRequest(
        @Schema(description = "Unique username", example = "jdoe")
                @NotBlank
                String username,
        @Schema(description = "Email address", example = "john.doe@example.com")
                @NotBlank
                @Email
                String email,
        @Schema(
                        description = "User password",
                        example = "StrongPass123!",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                String password,
        @Schema(description = "First name", example = "John")
                @NotBlank
                String firstName,
        @Schema(description = "Last name", example = "Doe")
                @NotBlank
                String lastName) {
}
