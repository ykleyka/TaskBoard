package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectSummaryReportAsyncService {
    private final AsyncTaskRegistry asyncTaskRegistry;
    private final ProjectSummaryReportService projectSummaryReportService;

    @Async("taskBoardAsyncExecutor")
    public CompletableFuture<ProjectSummaryReportResponse> generateProjectSummaryReport(
            String taskId, Long projectId) {
        asyncTaskRegistry.markRunning(taskId);
        ProjectSummaryReportResponse report =
                projectSummaryReportService.buildProjectSummaryReport(projectId);
        return CompletableFuture.completedFuture(report);
    }
}
