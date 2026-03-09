package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentPutRequest(@NotBlank String text) {

}
