package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.ProjectRole;

public record ProjectUserSummaryResponse(Long id, String username, ProjectRole role) {

}
