package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.DashboardResponse;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.mapper.ProjectMapper;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        long totalTasks = taskRepository.countAssignedVisibleToUser(currentUserId);
        long completedTasks =
                taskRepository.countAssignedVisibleToUserAndStatus(currentUserId, Status.COMPLETED);
        long overdueTasks = taskRepository.countOverdueAssignedVisibleToUser(
                currentUserId, now, Status.COMPLETED);
        long dueTodayTasks = taskRepository.countDueTodayAssignedVisibleToUser(
                currentUserId, todayStart, tomorrowStart, Status.COMPLETED);
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
                taskRepository.findUpcomingAssignedVisibleToUser(
                                currentUserId,
                                Status.COMPLETED,
                                PageRequest.of(
                                        0,
                                        UPCOMING_TASKS_LIMIT,
                                        Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("id"))))
                        .map(taskMapper::toResponse)
                        .getContent();

        return new DashboardResponse(
                projectRepository.countVisibleToUser(currentUserId),
                totalTasks,
                totalTasks - completedTasks,
                completedTasks,
                overdueTasks,
                dueTodayTasks,
                projectMemberRepository.countCollaboratorsVisibleToUser(currentUserId),
                recentProjects,
                upcomingTasks);
    }
}
