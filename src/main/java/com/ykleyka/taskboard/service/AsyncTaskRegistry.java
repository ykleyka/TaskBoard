package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.exception.AsyncTaskNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncOperationType;
import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;

@Service
public class AsyncTaskRegistry {
    private final ConcurrentMap<String, AsyncTaskDescriptor> tasks = new ConcurrentHashMap<>();
    private final AtomicLong submittedCount = new AtomicLong();
    private final AtomicLong runningCount = new AtomicLong();
    private final AtomicLong completedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicInteger projectSummaryAtomicCounter = new AtomicInteger();
    private volatile int projectSummaryUnsafeCounter;

    public AsyncTaskDescriptor create(AsyncOperationType operationType) {
        return create(operationType, null, null);
    }

    public AsyncTaskDescriptor create(
            AsyncOperationType operationType, Long ownerUserId, Long projectId) {
        AsyncTaskDescriptor descriptor =
                new AsyncTaskDescriptor(
                        java.util.UUID.randomUUID().toString(),
                        operationType,
                        ownerUserId,
                        projectId,
                        AsyncTaskStatus.SUBMITTED,
                        Instant.now(),
                        null,
                        null,
                        null,
                        null);
        tasks.put(descriptor.id(), descriptor);
        submittedCount.incrementAndGet();
        return descriptor;
    }

    public AsyncTaskDescriptor get(String taskId) {
        AsyncTaskDescriptor descriptor = tasks.get(taskId);
        if (descriptor == null) {
            throw new AsyncTaskNotFoundException(taskId);
        }
        return descriptor;
    }

    public void markRunning(String taskId) {
        update(taskId, current ->
                new AsyncTaskDescriptor(
                        current.id(),
                        current.operationType(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.RUNNING,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        null,
                        null,
                        null));
        runningCount.incrementAndGet();
    }

    public void complete(String taskId, Object result) {
        update(taskId, current ->
                new AsyncTaskDescriptor(
                        current.id(),
                        current.operationType(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.COMPLETED,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        Instant.now(),
                        null,
                        result));
        runningCount.accumulateAndGet(1L, (current, decrement) -> current > 0 ? current - 1 : 0);
        completedCount.incrementAndGet();
    }

    public void fail(String taskId, Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        update(taskId, current ->
                new AsyncTaskDescriptor(
                        current.id(),
                        current.operationType(),
                        current.ownerUserId(),
                        current.projectId(),
                        AsyncTaskStatus.FAILED,
                        current.createdAt(),
                        current.startedAt() == null ? Instant.now() : current.startedAt(),
                        Instant.now(),
                        resolveErrorMessage(rootCause),
                        null));
        runningCount.accumulateAndGet(1L, (current, decrement) -> current > 0 ? current - 1 : 0);
        failedCount.incrementAndGet();
    }

    public AsyncTaskMetrics metrics() {
        return new AsyncTaskMetrics(
                submittedCount.get(),
                runningCount.get(),
                completedCount.get(),
                failedCount.get(),
                projectSummaryUnsafeCounter,
                projectSummaryAtomicCounter.get(),
                projectSummaryUnsafeCounter < projectSummaryAtomicCounter.get());
    }

    public void recordProjectSummaryRequest() {
        incrementProjectSummaryUnsafeCounter();
        projectSummaryAtomicCounter.incrementAndGet();
    }

    public AsyncTaskStatusResponse<Object> toResponse(AsyncTaskDescriptor descriptor) {
        return new AsyncTaskStatusResponse<>(
                descriptor.id(),
                descriptor.operationType(),
                descriptor.status(),
                descriptor.createdAt(),
                descriptor.startedAt(),
                descriptor.completedAt(),
                descriptor.errorMessage(),
                descriptor.result());
    }

    private void update(String taskId, UnaryOperator<AsyncTaskDescriptor> updater) {
        tasks.compute(taskId, (key, current) -> {
            if (current == null) {
                throw new AsyncTaskNotFoundException(taskId);
            }
            return updater.apply(current);
        });
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

    private void incrementProjectSummaryUnsafeCounter() {
        projectSummaryUnsafeCounter++;
    }

    public record AsyncTaskDescriptor(
            String id,
            AsyncOperationType operationType,
            Long ownerUserId,
            Long projectId,
            AsyncTaskStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            String errorMessage,
            Object result) {
    }

    public record AsyncTaskMetrics(
            long submittedCount,
            long runningCount,
            long completedCount,
            long failedCount,
            int projectSummaryUnsafeCounter,
            int projectSummaryAtomicCounter,
            boolean raceConditionDetected) {
    }
}
