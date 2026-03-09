package com.ykleyka.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "username must not be blank") String username,
        @Pattern(regexp = ".*\\S.*", message = "email must not be blank") @Email String email,
        @Pattern(regexp = ".*\\S.*", message = "password must not be blank")
                @JsonAlias("passwordHash")
                String password,
        @Pattern(regexp = ".*\\S.*", message = "firstName must not be blank") String firstName,
        @Pattern(regexp = ".*\\S.*", message = "lastName must not be blank") String lastName) {}
