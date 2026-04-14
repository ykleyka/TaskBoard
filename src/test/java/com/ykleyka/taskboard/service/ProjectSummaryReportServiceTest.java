package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.dto.ProjectTaskSummaryResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectSummaryReportServiceTest {
    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectSummaryReportService service;

    @Test
    void buildProjectSummaryReport_whenProjectHasMixedTasks_calculatesAggregateFields() {
        Long projectId = 1L;
        Instant now = Instant.now();
        ProjectDetailsResponse details =
                new ProjectDetailsResponse(
                        projectId,
                        "Alpha",
                        "description",
                        now.minusSeconds(3600),
                        now.minusSeconds(60),
                        2,
                        3,
                        1,
                        List.of(
                                new ProjectUserSummaryResponse(1L, "owner", ProjectRole.OWNER),
                                new ProjectUserSummaryResponse(2L, "member", ProjectRole.MEMBER)),
                        List.of(
                                new ProjectTaskSummaryResponse(
                                        1L,
                                        "Overdue high",
                                        Status.TODO,
                                        Priority.HIGH,
                                        1L,
                                        "owner",
                                        2L,
                                        "member",
                                        now.minusSeconds(600),
                                        now.minusSeconds(7200),
                                        now.minusSeconds(120)),
                                new ProjectTaskSummaryResponse(
                                        2L,
                                        "Completed task",
                                        Status.COMPLETED,
                                        Priority.MEDIUM,
                                        1L,
                                        "owner",
                                        null,
                                        null,
                                        now.minusSeconds(300),
                                        now.minusSeconds(7200),
                                        now.minusSeconds(120)),
                                new ProjectTaskSummaryResponse(
                                        3L,
                                        "In progress",
                                        Status.IN_PROGRESS,
                                        Priority.LOW,
                                        1L,
                                        "owner",
                                        null,
                                        null,
                                        now.plusSeconds(3600),
                                        now.minusSeconds(7200),
                                        now.minusSeconds(120))));
        when(projectService.getProjectById(projectId)).thenReturn(details);

        ProjectSummaryReportResponse report = service.buildProjectSummaryReport(projectId);

        assertEquals(projectId, report.projectId());
        assertEquals(2, report.membersCount());
        assertEquals(3, report.tasksCount());
        assertEquals(1, report.completedTasksCount());
        assertEquals(1, report.overdueTasksCount());
        assertEquals(2, report.unassignedTasksCount());
        assertEquals(1, report.highPriorityTasksCount());
        assertEquals(1L, report.tasksByStatus().get(Status.TODO));
        assertEquals(1L, report.tasksByStatus().get(Status.IN_PROGRESS));
        assertEquals(1L, report.tasksByStatus().get(Status.COMPLETED));
        assertEquals(2, report.members().size());
        assertEquals("owner", report.members().getFirst().username());
        assertNotNull(report.generatedAt());
    }

    @Test
    void buildProjectSummaryReport_whenTasksHaveNullFields_initializesEmptyAggregates() {
        Long projectId = 2L;
        Instant now = Instant.now();
        ProjectDetailsResponse details =
                new ProjectDetailsResponse(
                        projectId,
                        "Beta",
                        null,
                        now.minusSeconds(3600),
                        now.minusSeconds(60),
                        1,
                        2,
                        0,
                        List.of(new ProjectUserSummaryResponse(1L, "owner", ProjectRole.OWNER)),
                        List.of(
                                new ProjectTaskSummaryResponse(
                                        10L,
                                        "No status and no due date",
                                        null,
                                        Priority.MEDIUM,
                                        1L,
                                        "owner",
                                        null,
                                        null,
                                        null,
                                        now.minusSeconds(7200),
                                        now.minusSeconds(120)),
                                new ProjectTaskSummaryResponse(
                                        11L,
                                        "Assigned low priority",
                                        Status.TODO,
                                        Priority.LOW,
                                        1L,
                                        "owner",
                                        1L,
                                        "owner",
                                        null,
                                        now.minusSeconds(7200),
                                        now.minusSeconds(120))));
        when(projectService.getProjectById(projectId)).thenReturn(details);

        ProjectSummaryReportResponse report = service.buildProjectSummaryReport(projectId);

        assertEquals(projectId, report.projectId());
        assertEquals(0, report.overdueTasksCount());
        assertEquals(1, report.unassignedTasksCount());
        assertEquals(0, report.highPriorityTasksCount());
        assertNull(report.nearestDueDate());
        assertEquals(1L, report.tasksByStatus().get(Status.TODO));
        assertEquals(0L, report.tasksByStatus().get(Status.IN_PROGRESS));
        assertEquals(0L, report.tasksByStatus().get(Status.COMPLETED));
    }
}
