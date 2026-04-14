package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.dto.AsyncTaskSubmissionResponse;
import com.ykleyka.taskboard.dto.AsyncTaskMetricsResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncTaskServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectSummaryReportAsyncService projectSummaryReportAsyncService;

    private AsyncTaskRegistry asyncTaskRegistry;
    private AsyncTaskService service;

    @BeforeEach
    void setUp() {
        asyncTaskRegistry = new AsyncTaskRegistry();
        service = new AsyncTaskService(
                asyncTaskRegistry,
                projectRepository,
                projectSummaryReportAsyncService);
    }

    @Test
    void submitProjectSummaryReport_whenProjectMissing_throwsNotFound() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProjectNotFoundException.class, () -> service.submitProjectSummaryReport(99L));

        verify(projectSummaryReportAsyncService, never()).generateProjectSummaryReport(anyString(), eq(99L));
    }

    @Test
    void submitProjectSummaryReport_whenCompleted_updatesAsyncTaskStatusWithResult() {
        Long projectId = 10L;
        ProjectSummaryReportResponse report =
                new ProjectSummaryReportResponse(
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
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectSummaryReportAsyncService.generateProjectSummaryReport(anyString(), eq(projectId)))
                .thenAnswer(invocation -> {
                    String taskId = invocation.getArgument(0);
                    asyncTaskRegistry.markRunning(taskId);
                    return CompletableFuture.completedFuture(report);
                });

        AsyncTaskSubmissionResponse submission = service.submitProjectSummaryReport(projectId);
        AsyncTaskStatusResponse<?> status = service.getAsyncTaskStatus(submission.asyncTaskId());
        AsyncTaskMetricsResponse metrics = service.getAsyncTaskMetrics();

        assertEquals(AsyncTaskStatus.SUBMITTED, submission.status());
        assertEquals(AsyncTaskStatus.COMPLETED, status.status());
        assertInstanceOf(ProjectSummaryReportResponse.class, status.result());
        assertEquals(report, status.result());
        assertEquals(1L, metrics.submittedCount());
        assertEquals(0L, metrics.runningCount());
        assertEquals(1L, metrics.completedCount());
        assertEquals(0L, metrics.failedCount());
        assertEquals(1, metrics.projectSummaryUnsafeCounter());
        assertEquals(1, metrics.projectSummaryAtomicCounter());
        assertEquals(false, metrics.raceConditionDetected());
    }

    @Test
    void submitProjectSummaryReport_whenAsyncExecutionFails_marksTaskAsFailed() {
        Long projectId = 11L;
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectSummaryReportAsyncService.generateProjectSummaryReport(anyString(), eq(projectId)))
                .thenAnswer(invocation -> {
                    String taskId = invocation.getArgument(0);
                    asyncTaskRegistry.markRunning(taskId);
                    return CompletableFuture.failedFuture(new IllegalStateException("report-failed"));
                });

        AsyncTaskSubmissionResponse submission = service.submitProjectSummaryReport(projectId);
        AsyncTaskStatusResponse<?> status = service.getAsyncTaskStatus(submission.asyncTaskId());
        AsyncTaskMetricsResponse metrics = service.getAsyncTaskMetrics();

        assertEquals(AsyncTaskStatus.FAILED, status.status());
        assertEquals("report-failed", status.errorMessage());
        assertEquals(1L, metrics.submittedCount());
        assertEquals(0L, metrics.runningCount());
        assertEquals(0L, metrics.completedCount());
        assertEquals(1L, metrics.failedCount());
        assertEquals(1, metrics.projectSummaryUnsafeCounter());
        assertEquals(1, metrics.projectSummaryAtomicCounter());
    }
}
