package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskPutRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {
    private static final Map<String, String> SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("title", "title"),
            Map.entry("description", "description"),
            Map.entry("status", "status"),
            Map.entry("priority", "priority"),
            Map.entry("projectId", "project.id"),
            Map.entry("creatorId", "creator.id"),
            Map.entry("creatorUsername", "creator.username"),
            Map.entry("assigneeId", "assignee.id"),
            Map.entry("assigneeUsername", "assignee.username"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updatedAt", "updatedAt"),
            Map.entry("dueDate", "dueDate"));

    private final TaskMapper mapper;
    private final TaskRepository repository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskService(
            TaskMapper mapper,
            TaskRepository repository,
            UserRepository userRepository,
            ProjectRepository projectRepository) {
        this.mapper = mapper;
        this.repository = repository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    public List<TaskResponse> getTasks(
            Status status, String assignee, String sortBy, Sort.Direction sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        List<Task> tasks;
        if (status != null && assignee != null && !assignee.isBlank()) {
            tasks = repository.findAllByStatusAndAssigneeUsernameIgnoreCase(status, assignee, sort);
        } else if (status != null) {
            tasks = repository.findAllByStatus(status, sort);
        } else if (assignee != null && !assignee.isBlank()) {
            tasks = repository.findAllByAssigneeUsernameIgnoreCase(assignee, sort);
        } else {
            tasks = repository.findAll(sort);
        }
        return tasks.stream().map(mapper::toResponse).toList();
    }

    private Sort buildSort(String sortBy, Sort.Direction sortDir) {
        String fieldPath = SORT_FIELDS.get(sortBy);
        if (fieldPath == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported sortBy: " + sortBy + ". Supported values: " + SORT_FIELDS.keySet());
        }
        return Sort.by(sortDir, fieldPath);
    }

    public TaskDetailsResponse getTaskById(Long id) {
        return mapper.toDetailsResponse(findDetailedTask(id));
    }

    private Task findTask(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Task findDetailedTask(Long id) {
        return repository.findDetailedById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = mapper.toEntity(request);
        task.setProject(findProject(request.projectId()));
        task.setCreator(findUser(request.creatorId()));
        task.setAssignee(request.assigneeId() == null ? null : findUser(request.assigneeId()));
        task.setUpdatedAt(Instant.now());
        return mapper.toResponse(repository.save(task));
    }

    public TaskResponse updateTask(Long id, TaskPutRequest request) {
        Task oldTask = findTask(id);
        Task newTask = mapper.toEntity(request);
        newTask.setId(oldTask.getId());
        newTask.setCreatedAt(oldTask.getCreatedAt());
        newTask.setCreator(oldTask.getCreator());
        newTask.setProject(findProject(request.projectId()));
        newTask.setAssignee(request.assigneeId() == null ? null : findUser(request.assigneeId()));
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
        if (request.projectId() != null) {
            task.setProject(findProject(request.projectId()));
        }
        if (request.assigneeId() != null) {
            task.setAssignee(findUser(request.assigneeId()));
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

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
