package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Dashboard summary for the authenticated user")
public record DashboardResponse(
        long totalProjects,
        long totalTasks,
        long activeTasks,
        long completedTasks,
        long overdueTasks,
        long dueTodayTasks,
        long collaboratorsCount,
        List<ProjectResponse> recentProjects,
        List<TaskResponse> upcomingTasks) {
}
