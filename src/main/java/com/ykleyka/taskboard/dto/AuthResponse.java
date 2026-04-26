package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Successful authentication response")
public record AuthResponse(
        @Schema(example = "Bearer")
                String tokenType,
        @Schema(description = "Bearer access token")
                String accessToken,
        Instant expiresAt,
        UserResponse user) {
}
