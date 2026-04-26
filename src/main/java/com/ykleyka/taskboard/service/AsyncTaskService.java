package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.dto.AsyncTaskSubmissionResponse;
import com.ykleyka.taskboard.dto.AsyncTaskMetricsResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.exception.AsyncTaskNotFoundException;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncOperationType;
import com.ykleyka.taskboard.repository.ProjectRepository;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncTaskService {
    private final AsyncTaskRegistry asyncTaskRegistry;
    private final ProjectRepository projectRepository;
    private final ProjectSummaryReportAsyncService projectSummaryReportAsyncService;

    public AsyncTaskSubmissionResponse submitProjectSummaryReport(Long projectId) {
        return submitProjectSummaryReport(projectId, null);
    }

    public AsyncTaskSubmissionResponse submitProjectSummaryReport(Long projectId, Long currentUserId) {
        ensureProjectExists(projectId);
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                asyncTaskRegistry.create(
                        AsyncOperationType.PROJECT_SUMMARY_REPORT,
                        currentUserId,
                        projectId);
        asyncTaskRegistry.recordProjectSummaryRequest();
        CompletableFuture<ProjectSummaryReportResponse> future =
                projectSummaryReportAsyncService.generateProjectSummaryReport(
                        descriptor.id(), projectId);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                asyncTaskRegistry.fail(descriptor.id(), throwable);
                return;
            }
            asyncTaskRegistry.complete(descriptor.id(), result);
        });
        return new AsyncTaskSubmissionResponse(
                descriptor.id(),
                descriptor.operationType(),
                descriptor.status(),
                descriptor.createdAt());
    }

    public AsyncTaskStatusResponse<Object> getAsyncTaskStatus(String taskId, Long currentUserId) {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor = asyncTaskRegistry.get(taskId);
        if (descriptor.ownerUserId() != null && !descriptor.ownerUserId().equals(currentUserId)) {
            throw new AsyncTaskNotFoundException(taskId);
        }
        return asyncTaskRegistry.toResponse(descriptor);
    }

    public AsyncTaskStatusResponse<Object> getAsyncTaskStatus(String taskId) {
        return asyncTaskRegistry.toResponse(asyncTaskRegistry.get(taskId));
    }

    public AsyncTaskMetricsResponse getAsyncTaskMetrics() {
        AsyncTaskRegistry.AsyncTaskMetrics metrics = asyncTaskRegistry.metrics();
        return new AsyncTaskMetricsResponse(
                metrics.submittedCount(),
                metrics.runningCount(),
                metrics.completedCount(),
                metrics.failedCount(),
                metrics.projectSummaryUnsafeCounter(),
                metrics.projectSummaryAtomicCounter(),
                metrics.raceConditionDetected());
    }

    private void ensureProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
    }
}
