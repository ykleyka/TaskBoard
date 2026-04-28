package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.DashboardResponse;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.mapper.ProjectMapper;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final int RECENT_PROJECTS_LIMIT = 5;
    private static final int UPCOMING_TASKS_LIMIT = 6;

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public DashboardResponse getDashboard(Long currentUserId) {
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant tomorrowStart = todayStart.plus(Duration.ofDays(1));
        List<Task> tasks = taskRepository.findAllVisibleToUserList(currentUserId);

        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == Status.COMPLETED)
                .count();
        long overdueTasks = tasks.stream()
                .filter(task -> isOverdue(task, now))
                .count();
        long dueTodayTasks = tasks.stream()
                .filter(task -> isDueToday(task, todayStart, tomorrowStart))
                .count();
        List<ProjectResponse> recentProjects =
                projectRepository.findAllVisibleToUser(
                                currentUserId,
                                PageRequest.of(
                                        0,
                                        RECENT_PROJECTS_LIMIT,
                                        Sort.by(Sort.Order.desc("updatedAt"))))
                        .map(projectMapper::toResponse)
                        .getContent();
        List<TaskResponse> upcomingTasks =
                tasks.stream()
                        .filter(task -> task.getDueDate() != null)
                        .filter(task -> task.getStatus() != Status.COMPLETED)
                        .sorted(Comparator.comparing(Task::getDueDate))
                        .limit(UPCOMING_TASKS_LIMIT)
                        .map(taskMapper::toResponse)
                        .toList();

        return new DashboardResponse(
                projectRepository.countVisibleToUser(currentUserId),
                tasks.size(),
                tasks.size() - completedTasks,
                completedTasks,
                overdueTasks,
                dueTodayTasks,
                projectMemberRepository.countCollaboratorsVisibleToUser(currentUserId),
                recentProjects,
                upcomingTasks);
    }

    private boolean isOverdue(Task task, Instant now) {
        return task.getDueDate() != null
                && task.getDueDate().isBefore(now)
                && task.getStatus() != Status.COMPLETED;
    }

    private boolean isDueToday(Task task, Instant todayStart, Instant tomorrowStart) {
        return task.getDueDate() != null
                && !task.getDueDate().isBefore(todayStart)
                && task.getDueDate().isBefore(tomorrowStart)
                && task.getStatus() != Status.COMPLETED;
    }
}
