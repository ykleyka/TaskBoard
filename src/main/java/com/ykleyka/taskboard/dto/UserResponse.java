package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "User response")
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Instant createdAt,
        Instant updatedAt) {

}
