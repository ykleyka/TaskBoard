package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Detailed project response")
public record ProjectDetailsResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        int membersCount,
        int tasksCount,
        int completedTasksCount,
        List<ProjectUserSummaryResponse> users,
        List<ProjectTaskSummaryResponse> tasks) {

}
