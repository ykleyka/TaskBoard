package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.AsyncTaskMetricsResponse;
import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.dto.AsyncTaskSubmissionResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.exception.AsyncTaskNotFoundException;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectSummaryReportTaskServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectSummaryReportService projectSummaryReportService;

    private ProjectSummaryReportTaskService service;

    @BeforeEach
    void setUp() {
        Executor sameThreadExecutor = Runnable::run;
        service = new ProjectSummaryReportTaskService(
                projectRepository,
                projectSummaryReportService,
                sameThreadExecutor);
    }

    @Test
    void submitProjectSummaryReport_whenProjectMissing_throwsNotFound() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProjectNotFoundException.class, () -> service.submitProjectSummaryReport(99L));

        verify(projectSummaryReportService, never()).buildProjectSummaryReport(99L);
    }

    @Test
    void submitProjectSummaryReport_whenCompleted_updatesAsyncTaskStatusWithResult() {
        Long projectId = 10L;
        ProjectSummaryReportResponse report = report(projectId);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectSummaryReportService.buildProjectSummaryReport(projectId)).thenReturn(report);

        AsyncTaskSubmissionResponse submission = service.submitProjectSummaryReport(projectId, 1L);
        AsyncTaskStatusResponse<?> status =
                service.getAsyncTaskStatus(submission.asyncTaskId(), 1L);
        AsyncTaskMetricsResponse metrics = service.getAsyncTaskMetrics();

        assertEquals(AsyncTaskStatus.SUBMITTED, submission.status());
        assertEquals(AsyncTaskStatus.COMPLETED, status.status());
        assertInstanceOf(ProjectSummaryReportResponse.class, status.result());
        assertEquals(report, status.result());
        assertEquals(0L, metrics.runningCount());
        assertEquals(1L, metrics.completedCount());
        assertEquals(0L, metrics.failedCount());
        assertEquals(1, metrics.projectSummaryUnsafeCounter());
    }

    @Test
    void submitProjectSummaryReport_whenAsyncExecutionFails_marksTaskAsFailed() {
        Long projectId = 11L;
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectSummaryReportService.buildProjectSummaryReport(projectId))
                .thenThrow(new IllegalStateException("report-failed"));

        AsyncTaskSubmissionResponse submission = service.submitProjectSummaryReport(projectId, 1L);
        AsyncTaskStatusResponse<?> status =
                service.getAsyncTaskStatus(submission.asyncTaskId(), 1L);
        AsyncTaskMetricsResponse metrics = service.getAsyncTaskMetrics();

        assertEquals(AsyncTaskStatus.FAILED, status.status());
        assertEquals("report-failed", status.errorMessage());
        assertEquals(0L, metrics.runningCount());
        assertEquals(0L, metrics.completedCount());
        assertEquals(1L, metrics.failedCount());
        assertEquals(1, metrics.projectSummaryUnsafeCounter());
    }

    @Test
    void getAsyncTaskStatus_whenRequestedByAnotherUser_throwsNotFound() {
        Long projectId = 12L;
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectSummaryReportService.buildProjectSummaryReport(projectId))
                .thenReturn(report(projectId));

        AsyncTaskSubmissionResponse submission = service.submitProjectSummaryReport(projectId, 1L);

        assertThrows(
                AsyncTaskNotFoundException.class,
                () -> service.getAsyncTaskStatus(submission.asyncTaskId(), 2L));
    }

    private ProjectSummaryReportResponse report(Long projectId) {
        return new ProjectSummaryReportResponse(
                projectId,
                "Project",
                "description",
                Instant.parse("2026-01-01T00:00:00Z"),
                2,
                3,
                1,
                1,
                1,
                1,
                Instant.parse("2026-01-02T00:00:00Z"),
                Map.of(Status.TODO, 2L),
                List.of(new ProjectUserSummaryResponse(1L, "owner", ProjectRole.OWNER)));
    }
}
