package com.ykleyka.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String name) {

}
