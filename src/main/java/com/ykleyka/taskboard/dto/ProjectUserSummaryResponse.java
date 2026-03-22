package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short project member representation")
public record ProjectUserSummaryResponse(Long id, String username, ProjectRole role) {

}
