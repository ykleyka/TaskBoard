package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thread-safe async task execution metrics")
public record AsyncTaskMetricsResponse(
        long submittedCount,
        long runningCount,
        long completedCount,
        long failedCount,
        int projectSummaryUnsafeCounter,
        int projectSummaryAtomicCounter,
        boolean raceConditionDetected) {
}
