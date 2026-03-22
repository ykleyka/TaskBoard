package com.ykleyka.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for creating or fully updating a user")
public record UserRequest(
        @Schema(description = "Unique username", example = "jdoe")
                @NotBlank
                String username,
        @Schema(description = "Email address", example = "john.doe@example.com")
                @NotBlank
                @Email
                String email,
        @Schema(description = "User password", example = "StrongPass123!", writeOnly = true)
                @NotBlank(groups = OnCreate.class)
                @JsonAlias("passwordHash")
                String password,
        @Schema(description = "First name", example = "John")
                @NotBlank
                String firstName,
        @Schema(description = "Last name", example = "Doe")
                @NotBlank
                String lastName) {

}
