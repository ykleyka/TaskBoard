package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
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
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> getTasks(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        return service.getTasks(status, assignee, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return service.getTaskById(id);
    }

    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        return service.createTask(request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskPutRequest request) {
        return service.updateTask(id, request);
    }

    @PatchMapping("/{id}")
    public TaskResponse patchTask(@PathVariable Long id, @RequestBody TaskPatchRequest request) {
        return service.patchTask(id, request);
    }

    @DeleteMapping("/{id}")
    public TaskResponse deleteTask(@PathVariable Long id) {
        return service.deleteTask(id);
    }
}
