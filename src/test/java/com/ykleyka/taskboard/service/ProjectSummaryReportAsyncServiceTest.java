package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectSummaryReportAsyncServiceTest {
    @Mock
    private AsyncTaskRegistry asyncTaskRegistry;

    @Mock
    private ProjectSummaryReportService projectSummaryReportService;

    @InjectMocks
    private ProjectSummaryReportAsyncService service;

    @Test
    void generateProjectSummaryReport_marksTaskRunningAndReturnsReport() {
        ProjectSummaryReportResponse report =
                new ProjectSummaryReportResponse(
                        1L,
                        "Alpha",
                        "description",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        2,
                        3,
                        1,
                        1,
                        1,
                        1,
                        Instant.parse("2026-01-02T00:00:00Z"),
                        Map.of(),
                        List.of());
        when(projectSummaryReportService.buildProjectSummaryReport(1L)).thenReturn(report);

        CompletableFuture<ProjectSummaryReportResponse> future =
                service.generateProjectSummaryReport("async-task-id", 1L);

        assertEquals(report, future.join());
        verify(asyncTaskRegistry).markRunning("async-task-id");
        verify(projectSummaryReportService).buildProjectSummaryReport(1L);
    }
}
