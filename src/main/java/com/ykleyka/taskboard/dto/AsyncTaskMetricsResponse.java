package com.ykleyka.taskboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Async task execution metrics")
public record AsyncTaskMetricsResponse(
        long runningCount,
        long completedCount,
        long failedCount,
        int submittedCounter,
        int projectSummaryUnsafeCounter) {
}