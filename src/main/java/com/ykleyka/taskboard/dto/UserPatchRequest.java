package com.ykleyka.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UserPatchRequest(
        @Pattern(regexp = "^\\s*\\S.*$", message = "username must not be blank") String username,
        @Pattern(regexp = "^\\s*\\S.*$", message = "email must not be blank") @Email String email,
        @Pattern(regexp = "^\\s*\\S.*$", message = "password must not be blank")
                @JsonAlias("passwordHash")
                String password,
        @Pattern(regexp = "^\\s*\\S.*$", message = "firstName must not be blank") String firstName,
        @Pattern(regexp = "^\\s*\\S.*$", message = "lastName must not be blank") String lastName) {

}
