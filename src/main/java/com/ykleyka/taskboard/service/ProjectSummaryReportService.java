package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.dto.ProjectTaskSummaryResponse;
import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectSummaryReportService {
    private final ProjectService projectService;

    public ProjectSummaryReportResponse buildProjectSummaryReport(Long projectId) {
        Instant now = Instant.now();
        ProjectDetailsResponse project = projectService.getProjectById(projectId);
        Map<Status, Long> tasksByStatus = initTasksByStatus();
        project.tasks().stream()
                .map(ProjectTaskSummaryResponse::status)
                .filter(Objects::nonNull)
                .forEach(status -> tasksByStatus.merge(status, 1L, Long::sum));

        long overdueTasksCount =
                project.tasks().stream()
                        .filter(task -> isOverdue(task, now))
                        .count();
        long unassignedTasksCount =
                project.tasks().stream()
                        .filter(task -> task.assigneeId() == null)
                        .count();
        long highPriorityTasksCount =
                project.tasks().stream()
                        .filter(task -> task.priority() == Priority.HIGH)
                        .count();
        Instant nearestDueDate =
                project.tasks().stream()
                        .map(ProjectTaskSummaryResponse::dueDate)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

        return new ProjectSummaryReportResponse(
                project.id(),
                project.name(),
                project.description(),
                now,
                project.membersCount(),
                project.tasksCount(),
                project.completedTasksCount(),
                overdueTasksCount,
                unassignedTasksCount,
                highPriorityTasksCount,
                nearestDueDate,
                tasksByStatus,
                project.users());
    }

    private Map<Status, Long> initTasksByStatus() {
        Map<Status, Long> tasksByStatus = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            tasksByStatus.put(status, 0L);
        }
        return tasksByStatus;
    }

    private boolean isOverdue(ProjectTaskSummaryResponse task, Instant now) {
        return task.dueDate() != null
                && task.dueDate().isBefore(now)
                && task.status() != Status.COMPLETED;
    }
}
