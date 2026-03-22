package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short task tag representation")
public record TaskTagSummaryResponse(Long id, String name) {

}
