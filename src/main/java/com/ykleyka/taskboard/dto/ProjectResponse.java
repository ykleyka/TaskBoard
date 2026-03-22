package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Project response")
public record ProjectResponse(
        Long id, String name, String description, Instant createdAt, Instant updatedAt) {

}
