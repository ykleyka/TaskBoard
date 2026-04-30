package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.AsyncTaskMetricsResponse;
import com.ykleyka.taskboard.dto.AsyncTaskStatusResponse;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.service.ProjectSummaryReportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/async-tasks")
@RequiredArgsConstructor
@Tag(name = "Async Tasks", description = "Operations for tracking asynchronous business tasks")
public class AsyncTaskController {
    private final ProjectSummaryReportTaskService service;

    @Operation(
            summary = "Get asynchronous task metrics",
            description = "Returns current asynchronous task metrics.")
    @GetMapping("/metrics")
    public AsyncTaskMetricsResponse getAsyncTaskMetrics() {
        return service.getAsyncTaskMetrics();
    }

    @Operation(
            summary = "Get asynchronous task status",
            description = "Returns execution status for the specified asynchronous task.")
    @GetMapping("/{asyncTaskId}")
    public AsyncTaskStatusResponse<Object> getAsyncTaskStatus(
            @PathVariable @NotBlank String asyncTaskId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getAsyncTaskStatus(asyncTaskId, currentUser.id());
    }
}
