package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(@NotNull Long authorId, @NotBlank String text) {

}
