package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.Pattern;

public record ProjectPatchRequest(
        @Pattern(regexp = "^\\s*\\S.*$", message = "name must not be blank") String name,
        String description) {

}
