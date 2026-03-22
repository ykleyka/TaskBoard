package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tag response")
public record TagResponse(Long id, String name, int usageCount) {

}
