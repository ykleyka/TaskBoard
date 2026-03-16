package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.service.TaskService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;

    @GetMapping
    public List<TaskResponse> getTasks(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getTasks(status, assignee, pageable);
    }

    @GetMapping("/{id}")
    public TaskDetailsResponse getTaskById(@PathVariable Long id) {
        return service.getTaskById(id);
    }

    @GetMapping("/search")
    public List<TaskResponse> searchTasks(
            @RequestParam Long projectId,
            @RequestParam String tagName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.searchTasksByProjectIdAndTag(
                projectId, tagName, status, assignee, pageable);
    }

    @GetMapping("/overdue")
    public List<TaskResponse> searchOverdueTasksNative(
            @RequestParam Long projectId,
            @RequestParam String tagName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) Instant dueBefore,
            @PageableDefault(page = 0, size = 20, sort = "dueDate") Pageable pageable) {
        return service.searchOverdueTasksByProjectIdAndTagNative(
                projectId, tagName, status, assignee, dueBefore, pageable);
    }

    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        return service.createTask(request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return service.updateTask(id, request);
    }

    @PatchMapping("/{id}")
    public TaskResponse patchTask(
            @PathVariable Long id, @Valid @RequestBody TaskPatchRequest request) {
        return service.patchTask(id, request);
    }

    @DeleteMapping("/{id}")
    public TaskResponse deleteTask(@PathVariable Long id) {
        return service.deleteTask(id);
    }
}
