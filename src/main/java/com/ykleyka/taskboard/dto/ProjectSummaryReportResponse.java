package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Aggregated project summary report")
public record ProjectSummaryReportResponse(
        Long projectId,
        String projectName,
        String projectDescription,
        Instant generatedAt,
        long membersCount,
        long tasksCount,
        long completedTasksCount,
        long overdueTasksCount,
        long unassignedTasksCount,
        long highPriorityTasksCount,
        Instant nearestDueDate,
        Map<Status, Long> tasksByStatus,
        List<ProjectUserSummaryResponse> members) {
}
