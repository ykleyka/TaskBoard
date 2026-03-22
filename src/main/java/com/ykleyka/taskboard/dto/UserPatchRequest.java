package com.ykleyka.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload for partial user update")
public record UserPatchRequest(
        @Schema(description = "Unique username", example = "jdoe")
                @Pattern(regexp = "^\\s*\\S.*$", message = "username must not be blank")
                String username,
        @Schema(description = "Email address", example = "john.doe@example.com")
                @Pattern(regexp = "^\\s*\\S.*$", message = "email must not be blank")
                @Email
                String email,
        @Schema(description = "User password", example = "StrongPass123!", writeOnly = true)
                @Pattern(regexp = "^\\s*\\S.*$", message = "password must not be blank")
                @JsonAlias("passwordHash")
                String password,
        @Schema(description = "First name", example = "John")
                @Pattern(regexp = "^\\s*\\S.*$", message = "firstName must not be blank")
                String firstName,
        @Schema(description = "Last name", example = "Doe")
                @Pattern(regexp = "^\\s*\\S.*$", message = "lastName must not be blank")
                String lastName) {

}
