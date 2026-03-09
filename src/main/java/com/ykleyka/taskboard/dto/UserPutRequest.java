package com.ykleyka.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserPutRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @JsonAlias("passwordHash") String password,
        @NotBlank String firstName,
        @NotBlank String lastName) {

}
