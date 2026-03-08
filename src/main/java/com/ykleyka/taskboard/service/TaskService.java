package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id",
            "title",
            "description",
            "status",
            "priority",
            "assignee",
            "creator",
            "createdAt",
            "updatedAt",
            "dueDate");

    private final TaskMapper mapper;
    private final TaskRepository repository;

    public TaskService(TaskMapper mapper, TaskRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    public List<TaskResponse> getTasks(
            Status status, String assignee, String sortBy, Sort.Direction sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        List<Task> tasks;
        if (status != null && assignee != null && !assignee.isBlank()) {
            tasks = repository.findAllByStatusAndAssigneeIgnoreCase(status, assignee, sort);
        } else if (status != null) {
            tasks = repository.findAllByStatus(status, sort);
        } else if (assignee != null && !assignee.isBlank()) {
            tasks = repository.findAllByAssigneeIgnoreCase(assignee, sort);
        } else {
            tasks = repository.findAll(sort);
        }
        return tasks.stream().map(mapper::toResponse).toList();
    }

    private Sort buildSort(String sortBy, Sort.Direction sortDir) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sortBy: " + sortBy + ". Supported values: " + SORTABLE_FIELDS);
        }
        return Sort.by(sortDir, sortBy);
    }

    public TaskResponse getTaskById(Long id) {
        return mapper.toResponse(findTask(id));
    }

    private Task findTask(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = mapper.toEntity(request);
        task.setUpdatedAt(Instant.now());
        return mapper.toResponse(repository.save(task));
    }

    public TaskResponse updateTask(Long id, TaskPutRequest request) {
        Task oldTask = findTask(id);
        Task newTask = mapper.toEntity(request);
        newTask.setId(oldTask.getId());
        newTask.setCreatedAt(oldTask.getCreatedAt());
        newTask.setCreator(oldTask.getCreator());
        return mapper.toResponse(repository.save(newTask));
    }

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
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        task.setUpdatedAt(Instant.now());
        return mapper.toResponse(repository.save(task));
    }

    public TaskResponse deleteTask(Long id) {
        Task task = findTask(id);
        repository.delete(task);
        return mapper.toResponse(task);
    }
}
