package com.ykleyka.taskboard.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single validation error item")
public record ApiValidationError(
        @Schema(description = "Field or parameter name", example = "projectId")
                String field,
        @Schema(description = "Validation message", example = "must be greater than 0")
                String message,
        @Schema(description = "Rejected value", example = "0")
                String rejectedValue) {

}
