package com.ykleyka.taskboard.mapper;

import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.model.Task;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssignee(request.assignee());
        task.setCreator(request.creator());
        task.setStatus(Status.TODO);
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    public Task toEntity(TaskPutRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssignee(request.assignee());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee(),
                task.getCreator(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
