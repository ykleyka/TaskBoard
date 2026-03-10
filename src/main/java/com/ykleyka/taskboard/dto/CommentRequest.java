package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(Long authorId, @NotBlank String text) {

}
