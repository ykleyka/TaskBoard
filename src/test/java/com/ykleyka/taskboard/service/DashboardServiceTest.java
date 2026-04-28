package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.DashboardResponse;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.mapper.ProjectMapper;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getDashboard_countsVisibleTasksAndMapsRecentProjectsAndUpcomingTasks() {
        Long currentUserId = 7L;
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant todayDue =
                LocalDate.now(zone)
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .minus(Duration.ofSeconds(1));
        Task overdue = task(1L, "overdue", Status.TODO, now.minus(Duration.ofDays(1)));
        Task dueToday = task(2L, "today", Status.IN_PROGRESS, todayDue);
        Task future = task(3L, "future", Status.TODO, now.plus(Duration.ofDays(3)));
        Task completed = task(4L, "done", Status.COMPLETED, now.minus(Duration.ofDays(2)));
        Task withoutDueDate = task(5L, "without due", Status.TODO, null);
        Project project = project(10L, "Recent project");
        ProjectResponse projectResponse =
                new ProjectResponse(10L, "Recent project", "description", now, now);
        TaskResponse overdueResponse = taskResponse(1L, "overdue", overdue.getDueDate(), true);
        TaskResponse dueTodayResponse = taskResponse(2L, "today", dueToday.getDueDate(), false);
        TaskResponse futureResponse = taskResponse(3L, "future", future.getDueDate(), false);

        when(taskRepository.findAllVisibleToUserList(currentUserId))
                .thenReturn(List.of(future, completed, withoutDueDate, overdue, dueToday));
        when(projectRepository.findAllVisibleToUser(eq(currentUserId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));
        when(projectMapper.toResponse(project)).thenReturn(projectResponse);
        when(taskMapper.toResponse(overdue)).thenReturn(overdueResponse);
        when(taskMapper.toResponse(dueToday)).thenReturn(dueTodayResponse);
        when(taskMapper.toResponse(future)).thenReturn(futureResponse);
        when(projectRepository.countVisibleToUser(currentUserId)).thenReturn(3L);
        when(projectMemberRepository.countCollaboratorsVisibleToUser(currentUserId)).thenReturn(4L);

        DashboardResponse response = service.getDashboard(currentUserId);

        assertEquals(3L, response.totalProjects());
        assertEquals(5L, response.totalTasks());
        assertEquals(4L, response.activeTasks());
        assertEquals(1L, response.completedTasks());
        assertEquals(1L, response.overdueTasks());
        assertEquals(1L, response.dueTodayTasks());
        assertEquals(4L, response.collaboratorsCount());
        assertEquals(List.of(projectResponse), response.recentProjects());
        assertEquals(
                List.of(overdueResponse, dueTodayResponse, futureResponse),
                response.upcomingTasks());
    }

    @Test
    void getDashboard_limitsUpcomingTasksToSixNearestDeadlines() {
        Long currentUserId = 8L;
        Instant base = Instant.now().plus(Duration.ofDays(1));
        List<Task> tasks = List.of(
                task(1L, "first", Status.TODO, base.plus(Duration.ofHours(1))),
                task(2L, "second", Status.TODO, base.plus(Duration.ofHours(2))),
                task(3L, "third", Status.TODO, base.plus(Duration.ofHours(3))),
                task(4L, "fourth", Status.TODO, base.plus(Duration.ofHours(4))),
                task(5L, "fifth", Status.TODO, base.plus(Duration.ofHours(5))),
                task(6L, "sixth", Status.TODO, base.plus(Duration.ofHours(6))),
                task(7L, "seventh", Status.TODO, base.plus(Duration.ofHours(7))));

        when(taskRepository.findAllVisibleToUserList(currentUserId)).thenReturn(tasks);
        when(projectRepository.findAllVisibleToUser(eq(currentUserId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(projectRepository.countVisibleToUser(currentUserId)).thenReturn(0L);
        when(projectMemberRepository.countCollaboratorsVisibleToUser(currentUserId)).thenReturn(0L);
        for (Task task : tasks.subList(0, 6)) {
            when(taskMapper.toResponse(task))
                    .thenReturn(taskResponse(task.getId(), task.getTitle(), task.getDueDate(), false));
        }

        DashboardResponse response = service.getDashboard(currentUserId);

        assertEquals(6, response.upcomingTasks().size());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L), response.upcomingTasks().stream()
                .map(TaskResponse::id)
                .toList());
    }

    private Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("description");
        project.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        project.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return project;
    }

    private Task task(Long id, String title, Status status, Instant dueDate) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription("description");
        task.setStatus(status);
        task.setPriority(Priority.MEDIUM);
        task.setDueDate(dueDate);
        task.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return task;
    }

    private TaskResponse taskResponse(Long id, String title, Instant dueDate, boolean overdue) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new TaskResponse(
                id,
                title,
                "description",
                Status.TODO,
                Priority.MEDIUM,
                10L,
                "project",
                1L,
                "creator",
                2L,
                "assignee",
                dueDate,
                overdue,
                now,
                now);
    }
}
