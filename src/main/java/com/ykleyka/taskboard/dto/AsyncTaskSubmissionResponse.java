package com.ykleyka.taskboard.dto;

import com.ykleyka.taskboard.model.enums.AsyncTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Response returned after asynchronous task submission")
public record AsyncTaskSubmissionResponse(
        String asyncTaskId,
        AsyncTaskStatus status,
        Instant createdAt) {
}
