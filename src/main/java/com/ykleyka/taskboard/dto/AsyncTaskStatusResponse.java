package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Current status of an asynchronous task")
public record AsyncTaskStatusResponse<T>(
        String asyncTaskId,
        AsyncTaskStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        T result) {
}
