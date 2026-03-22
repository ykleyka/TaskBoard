package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for creating or updating a comment")
public record CommentRequest(
        @Schema(description = "Author identifier", example = "3")
                @NotNull(groups = OnCreate.class)
                @Positive
                Long authorId,
        @Schema(description = "Comment text", example = "Need to clarify the deadline.")
                @NotBlank
                String text) {

}
