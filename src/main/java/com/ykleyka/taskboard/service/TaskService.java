package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Status;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Service layer for task operations. */
@Service
public class TaskService {
  private final TaskMapper mapper;
  private final TaskRepository repository;

  /** Creates service instance. */
  public TaskService(TaskMapper mapper, TaskRepository repository) {
    this.mapper = mapper;
    this.repository = repository;
    seedInitialTasks();
  }

  /** Returns tasks with optional filtering by status and assignee. */
  public List<TaskResponse> getTasks(Status status, String assignee) {
    List<TaskResponse> result = new ArrayList<>();

    for (Task task : repository.findAll()) {
      boolean statusMatches = status == null || task.getStatus() == status;
      boolean assigneeMatches =
          assignee == null || assignee.isBlank() || assignee.equalsIgnoreCase(task.getAssignee());

      if (statusMatches && assigneeMatches) {
        result.add(mapper.toResponse(task));
      }
    }

    return result;
  }

  /** Returns task by id. */
  public TaskResponse getTaskById(Long id) {
    return mapper.toResponse(findTask(id));
  }

  private Task findTask(Long id) {
    return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }

  /** Creates a new task. */
  public TaskResponse createTask(TaskRequest request) {
    Task task = mapper.toEntity(request);
    task.setUpdatedAt(LocalDateTime.now());
    repository.save(task);
    return mapper.toResponse(task);
  }

  private void seedInitialTasks() {
    createTask(
        new TaskRequest(
            "Create project",
            "Create Java Spring Boot project and install dependencies",
            "Valik"));
    createTask(
        new TaskRequest(
            "Add Task entity",
            "Add Task entity and implement controller and service",
            "Valik"));
  }

  /** Replaces full task payload by id. */
  public TaskResponse updateTask(Long id, TaskPutRequest request) {
    Task oldTask = findTask(id);
    Task newTask = mapper.toEntity(request);
    newTask.setId(oldTask.getId());
    newTask.setCreatedAt(oldTask.getCreatedAt());

    if (!repository.replace(id, newTask)) {
      throw new TaskNotFoundException(id);
    }

    return mapper.toResponse(newTask);
  }

  /** Partially updates task fields by id. */
  public TaskResponse patchTask(Long id, TaskPatchRequest request) {
    Task task = findTask(id);

    if (request.title() != null) {
      task.setTitle(request.title());
    }
    if (request.description() != null) {
      task.setDescription(request.description());
    }
    if (request.status() != null) {
      task.setStatus(request.status());
    }
    if (request.assignee() != null) {
      task.setAssignee(request.assignee());
    }

    task.setUpdatedAt(LocalDateTime.now());
    return mapper.toResponse(task);
  }

  /** Deletes task by id. */
  public TaskResponse deleteTask(Long id) {
    Task task = findTask(id);

    if (!repository.deleteById(id)) {
      throw new TaskNotFoundException(id);
    }

    return mapper.toResponse(task);
  }
}
