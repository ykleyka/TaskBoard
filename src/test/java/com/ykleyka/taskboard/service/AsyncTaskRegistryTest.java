package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ykleyka.taskboard.dto.ProjectSummaryReportResponse;
import com.ykleyka.taskboard.exception.AsyncTaskNotFoundException;
import com.ykleyka.taskboard.model.enums.AsyncOperationType;
import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AsyncTaskRegistryTest {
    private final AsyncTaskRegistry registry = new AsyncTaskRegistry();

    @Test
    void get_whenTaskDoesNotExist_throwsNotFound() {
        assertThrows(AsyncTaskNotFoundException.class, () -> registry.get("missing-task"));
    }

    @Test
    void markRunning_whenCalledTwice_preservesStartedAt() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.markRunning(descriptor.id());
        Instant startedAtAfterFirstMarkRunning = registry.get(descriptor.id()).startedAt();
        registry.markRunning(descriptor.id());

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.RUNNING, actual.status());
        assertEquals(startedAtAfterFirstMarkRunning, actual.startedAt());
        assertEquals(2L, registry.metrics().runningCount());
    }

    @Test
    void lifecycle_whenTaskCompletes_updatesStatusTimestampsAndResult() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);
        ProjectSummaryReportResponse report =
                new ProjectSummaryReportResponse(
                        1L,
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
                        java.util.List.of());

        registry.markRunning(descriptor.id());
        registry.complete(descriptor.id(), report);

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.COMPLETED, actual.status());
        assertNotNull(actual.startedAt());
        assertNotNull(actual.completedAt());
        assertEquals(report, actual.result());
        assertNull(actual.errorMessage());
        AsyncTaskRegistry.AsyncTaskMetrics metrics = registry.metrics();
        assertEquals(1L, metrics.submittedCount());
        assertEquals(0L, metrics.runningCount());
        assertEquals(1L, metrics.completedCount());
        assertEquals(0L, metrics.failedCount());
        assertEquals(0, metrics.projectSummaryUnsafeCounter());
        assertEquals(0, metrics.projectSummaryAtomicCounter());
    }

    @Test
    void complete_whenTaskWasNotMarkedRunning_initializesStartedAt() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.complete(descriptor.id(), "done");

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.COMPLETED, actual.status());
        assertNotNull(actual.startedAt());
        assertNotNull(actual.completedAt());
        assertEquals("done", actual.result());
        assertEquals(0L, registry.metrics().runningCount());
        assertEquals(1L, registry.metrics().completedCount());
    }

    @Test
    void fail_whenCompletionExceptionProvided_usesRootCauseMessage() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.markRunning(descriptor.id());
        registry.fail(descriptor.id(), new CompletionException(new RuntimeException("boom")));

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.FAILED, actual.status());
        assertEquals("boom", actual.errorMessage());
        assertNotNull(actual.completedAt());
        assertNull(actual.result());
        AsyncTaskRegistry.AsyncTaskMetrics metrics = registry.metrics();
        assertEquals(1L, metrics.submittedCount());
        assertEquals(0L, metrics.runningCount());
        assertEquals(0L, metrics.completedCount());
        assertEquals(1L, metrics.failedCount());
        assertEquals(0, metrics.projectSummaryUnsafeCounter());
        assertEquals(0, metrics.projectSummaryAtomicCounter());
    }

    @Test
    void fail_whenThrowableMessageIsBlank_usesThrowableClassName() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.fail(descriptor.id(), new IllegalStateException(" "));

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.FAILED, actual.status());
        assertNotNull(actual.startedAt());
        assertNotNull(actual.completedAt());
        assertEquals("IllegalStateException", actual.errorMessage());
        assertEquals(1L, registry.metrics().failedCount());
    }

    @Test
    void fail_whenCompletionExceptionHasNoCause_usesCompletionExceptionMessage() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.fail(descriptor.id(), new CompletionException("wrapped-without-cause", null));

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.FAILED, actual.status());
        assertEquals("wrapped-without-cause", actual.errorMessage());
    }

    @Test
    void fail_whenThrowableMessageIsNull_usesThrowableClassName() {
        AsyncTaskRegistry.AsyncTaskDescriptor descriptor =
                registry.create(AsyncOperationType.PROJECT_SUMMARY_REPORT);

        registry.fail(descriptor.id(), new RuntimeException());

        AsyncTaskRegistry.AsyncTaskDescriptor actual = registry.get(descriptor.id());

        assertEquals(AsyncTaskStatus.FAILED, actual.status());
        assertEquals("RuntimeException", actual.errorMessage());
    }

    @Test
    void markRunning_whenTaskDoesNotExist_throwsNotFound() {
        assertThrows(AsyncTaskNotFoundException.class, () -> registry.markRunning("missing-task"));
    }

    @Test
    void recordProjectSummaryRequest_whenCalledConcurrently_detectsRaceCondition() throws InterruptedException {
        int threadCount = 128;
        int incrementsPerThread = 5_000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    readyLatch.countDown();
                    await(startLatch);
                    for (int increment = 0; increment < incrementsPerThread; increment++) {
                        registry.recordProjectSummaryRequest();
                    }
                    doneLatch.countDown();
                });
            }

            await(readyLatch);
            startLatch.countDown();
            await(doneLatch);
        } finally {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }

        AsyncTaskRegistry.AsyncTaskMetrics metrics = registry.metrics();
        int expectedAtomicCounter = threadCount * incrementsPerThread;
        assertEquals(expectedAtomicCounter, metrics.projectSummaryAtomicCounter());
        assertTrue(metrics.projectSummaryUnsafeCounter() < expectedAtomicCounter);
        assertTrue(metrics.raceConditionDetected());
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
