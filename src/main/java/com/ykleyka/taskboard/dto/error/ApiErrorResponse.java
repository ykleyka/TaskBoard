package com.ykleyka.taskboard.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Unified API error response")
public record ApiErrorResponse(
        @Schema(description = "Timestamp when the error occurred", example = "2026-03-18T11:25:43Z")
                Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
                int status,
        @Schema(description = "HTTP error reason", example = "Bad Request")
                String error,
        @Schema(description = "Application error message", example = "Request validation failed")
                String message,
        @Schema(description = "Request path", example = "/api/tasks")
                String path,
        @Schema(description = "Detailed validation errors")
                List<ApiValidationError> errors) {

    public ApiErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
