package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.service.TaskService;
import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Operations for managing tasks and task searches")
public class TaskController {
    private final TaskService service;

    @Operation(summary = "List tasks", description = "Returns tasks with optional status and assignee filters.")
    @GetMapping
    public List<TaskResponse> getTasks(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @ParameterObject @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getTasks(status, assignee, currentUser.id(), pageable);
    }

    @Operation(summary = "Get task by id", description = "Returns detailed task information.")
    @GetMapping("/{id}")
    public TaskDetailsResponse getTaskById(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getTaskById(id, currentUser.id());
    }

    @Operation(summary = "Search tasks by project and tag",
            description = "Searches tasks by project, tag and optional filters.")
    @GetMapping("/search")
    public List<TaskResponse> searchTasks(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @Positive Long projectId,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return service.searchTasksByProjectIdAndTag(
                projectId, tagName, status, assignee, pageable, currentUser.id());
    }

    @Operation(summary = "Search overdue tasks",
            description = "Searches overdue tasks using the native query endpoint.")
    @GetMapping("/overdue")
    public List<TaskResponse> searchOverdueTasksNative(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @Positive Long projectId,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) Instant dueBefore,
            @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return service.searchOverdueTasksByProjectIdAndTagNative(
                projectId, tagName, status, assignee, dueBefore, pageable, currentUser.id());
    }

    @Operation(summary = "Create task", description = "Creates a new task.")
    @PostMapping
    public TaskResponse createTask(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Validated({Default.class, OnCreate.class}) @RequestBody TaskRequest request) {
        return service.createTask(request, currentUser.id());
    }

    @Operation(summary = "Replace task", description = "Fully updates an existing task.")
    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TaskRequest request) {
        return service.updateTask(id, request, currentUser.id());
    }

    @Operation(summary = "Patch task", description = "Partially updates an existing task.")
    @PatchMapping("/{id}")
    public TaskResponse patchTask(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TaskPatchRequest request) {
        return service.patchTask(id, request, currentUser.id());
    }

    @Operation(summary = "Delete task", description = "Deletes a task and returns the removed entity.")
    @DeleteMapping("/{id}")
    public TaskResponse deleteTask(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.deleteTask(id, currentUser.id());
    }
}
