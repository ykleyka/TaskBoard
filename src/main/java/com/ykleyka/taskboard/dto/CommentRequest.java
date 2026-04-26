package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for creating or updating a comment")
public record CommentRequest(
        @Schema(description = "Legacy author identifier. Authenticated clients can omit it.", example = "3")
                @Positive
                Long authorId,
        @Schema(description = "Comment text", example = "Need to clarify the deadline.")
                @NotBlank
                String text) {

}
