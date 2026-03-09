package com.ykleyka.taskboard.mapper;

import com.ykleyka.taskboard.dto.TaskCommentSummaryResponse;
import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.dto.TaskTagSummaryResponse;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
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
                task.getProject() == null ? null : task.getProject().getId(),
                task.getProject() == null ? null : task.getProject().getName(),
                task.getCreator() == null ? null : task.getCreator().getId(),
                task.getCreator() == null ? null : task.getCreator().getUsername(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getUsername(),
                task.getDueDate(),
                task.getDueDate() != null
                        && task.getDueDate().isBefore(Instant.now())
                        && task.getStatus() != Status.COMPLETED,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    public TaskDetailsResponse toDetailsResponse(Task task) {
        List<TaskTagSummaryResponse> tags =
                task.getTags().stream()
                        .map(tag -> new TaskTagSummaryResponse(tag.getId(), tag.getName()))
                        .sorted(Comparator.comparing(TaskTagSummaryResponse::id))
                        .toList();

        List<TaskCommentSummaryResponse> comments =
                task.getComments().stream()
                        .map(
                                comment ->
                                        new TaskCommentSummaryResponse(
                                                comment.getId(),
                                                comment.getText(),
                                                comment.getAuthor() == null
                                                        ? null
                                                        : comment.getAuthor().getId(),
                                                comment.getAuthor() == null
                                                        ? null
                                                        : comment.getAuthor().getUsername(),
                                                comment.getCreatedAt(),
                                                comment.getUpdatedAt()))
                        .sorted(Comparator.comparing(TaskCommentSummaryResponse::id))
                        .toList();

        return new TaskDetailsResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getProject() == null ? null : task.getProject().getId(),
                task.getCreator() == null ? null : task.getCreator().getId(),
                task.getCreator() == null ? null : task.getCreator().getUsername(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getAssignee() == null ? null : task.getAssignee().getUsername(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                tags,
                comments);
    }
}
