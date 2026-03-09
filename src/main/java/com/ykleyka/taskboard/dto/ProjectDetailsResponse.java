package com.ykleyka.taskboard.dto;

import java.time.Instant;
import java.util.List;

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
