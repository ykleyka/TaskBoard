package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.AsyncTaskMetricsResponse;
import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.dto.AsyncTaskSubmissionResponse;
import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.exception.AsyncTaskNotFoundException;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncOperationType;
import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import com.ykleyka.taskboard.repository.ProjectRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ProjectSummaryReportTaskService {
    private final ConcurrentMap<String, ProjectSummaryReportTask> tasks = new ConcurrentHashMap<>();
    private final ProjectRepository projectRepository;
    private final ProjectSummaryReportService projectSummaryReportService;
    private final Executor taskBoardAsyncExecutor;
    private volatile int projectSummaryUnsafeCounter;

    public ProjectSummaryReportTaskService(
            ProjectRepository projectRepository,
            ProjectSummaryReportService projectSummaryReportService,
            @Qualifier("taskBoardAsyncExecutor") Executor taskBoardAsyncExecutor) {
        this.projectRepository = projectRepository;
        this.projectSummaryReportService = projectSummaryReportService;
        this.taskBoardAsyncExecutor = taskBoardAsyncExecutor;
    }

    public AsyncTaskSubmissionResponse submitProjectSummaryReport(Long projectId) {
        return submitProjectSummaryReport(projectId, null);
    }

    public AsyncTaskSubmissionResponse submitProjectSummaryReport(
            Long projectId, Long currentUserId) {
        ensureProjectExists(projectId);
        ProjectSummaryReportTask task =
                new ProjectSummaryReportTask(
                        UUID.randomUUID().toString(),
                        currentUserId,
                        projectId,
                        AsyncTaskStatus.SUBMITTED,
                        Instant.now(),
                        null,
                        null,
                        null,
                        null);
        tasks.put(task.id(), task);
        projectSummaryUnsafeCounter++;

        try {
            CompletableFuture
                    .runAsync(
                            () -> generateProjectSummaryReport(task.id(), projectId),
                            taskBoardAsyncExecutor)
                    .exceptionally(throwable -> {
                        fail(task.id(), throwable);
                        return null;
                    });
        } catch (RuntimeException exception) {
            fail(task.id(), exception);
        }

        return new AsyncTaskSubmissionResponse(
                task.id(),
                AsyncOperationType.PROJECT_SUMMARY_REPORT,
                task.status(),
                task.createdAt());
    }

    public AsyncTaskStatusResponse<Object> getAsyncTaskStatus(String taskId, Long currentUserId) {
        ProjectSummaryReportTask task = get(taskId);
        if (task.ownerUserId() != null && !task.ownerUserId().equals(currentUserId)) {
            throw new AsyncTaskNotFoundException(taskId);
        }
        return toResponse(task);
    }

    public AsyncTaskStatusResponse<Object> getAsyncTaskStatus(String taskId) {
        return toResponse(get(taskId));
    }

    public AsyncTaskMetricsResponse getAsyncTaskMetrics() {
        return new AsyncTaskMetricsResponse(
                countTasksByStatus(AsyncTaskStatus.RUNNING),
                countTasksByStatus(AsyncTaskStatus.COMPLETED),
                countTasksByStatus(AsyncTaskStatus.FAILED),
                projectSummaryUnsafeCounter);
    }

    private void generateProjectSummaryReport(String taskId, Long projectId) {
        markRunning(taskId);
        ProjectSummaryReportResponse report =
                projectSummaryReportService.buildProjectSummaryReport(projectId);
        complete(taskId, report);
    }

    private ProjectSummaryReportTask get(String taskId) {
        ProjectSummaryReportTask task = tasks.get(taskId);
        if (task == null) {
            throw new AsyncTaskNotFoundException(taskId);
        }
        return task;
    }

    private void markRunning(String taskId) {
        update(taskId, current ->
                new ProjectSummaryReportTask(
                        current.id(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.RUNNING,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        null,
                        null,
                        null));
    }

    private void complete(String taskId, ProjectSummaryReportResponse result) {
        update(taskId, current ->
                new ProjectSummaryReportTask(
                        current.id(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.COMPLETED,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        Instant.now(),
                        null,
                        result));
    }

    private void fail(String taskId, Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        update(taskId, current ->
                new ProjectSummaryReportTask(
                        current.id(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.FAILED,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        Instant.now(),
                        resolveErrorMessage(rootCause),
                        null));
    }

    private void update(String taskId, UnaryOperator<ProjectSummaryReportTask> updater) {
        tasks.compute(taskId, (key, current) -> {
            if (current == null) {
                throw new AsyncTaskNotFoundException(taskId);
            }
            return updater.apply(current);
        });
    }

    private AsyncTaskStatusResponse<Object> toResponse(ProjectSummaryReportTask task) {
        return new AsyncTaskStatusResponse<>(
                task.id(),
                AsyncOperationType.PROJECT_SUMMARY_REPORT,
                task.status(),
                task.createdAt(),
                task.startedAt(),
                task.completedAt(),
                task.errorMessage(),
                task.result());
    }

    private long countTasksByStatus(AsyncTaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.status() == status)
                .count();
    }

    private void ensureProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException
                && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    private String resolveErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private record ProjectSummaryReportTask(
            String id,
            Long ownerUserId,
            Long projectId,
            AsyncTaskStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            String errorMessage,
            ProjectSummaryReportResponse result) {
    }
}
