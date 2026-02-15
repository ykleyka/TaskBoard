package com.ykleyka.taskboard.mapper;

import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.Status;
import com.ykleyka.taskboard.model.Task;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Maps between task domain objects and DTOs. */
@Component
public class TaskMapper {

  /** Maps create request to a new task entity. */
  public Task toEntity(TaskRequest request) {
    Task task = new Task();
    task.setTitle(request.title());
    task.setDescription(request.description());
    task.setAssignee(request.assignee());
    task.setStatus(Status.TODO);
    LocalDateTime now = LocalDateTime.now();
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    return task;
  }

  /** Maps put request to a task entity. */
  public Task toEntity(TaskPutRequest request) {
    Task task = new Task();
    task.setTitle(request.title());
    task.setDescription(request.description());
    task.setAssignee(request.assignee());
    task.setStatus(request.status());
    LocalDateTime now = LocalDateTime.now();
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    return task;
  }

  /** Maps task entity to response DTO. */
  public TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getAssignee(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
