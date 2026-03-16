package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskSearchCache;
import com.ykleyka.taskboard.cache.TaskSearchCache.TaskSearchKey;
import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPatchRequest;
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
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper mapper;
    private final TaskRepository repository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCache projectCache;
    private final TagCache tagCache;
    private final CommentCache commentCache;
    private final TaskSearchCache searchCache;

    public List<TaskResponse> getTasks(
            Status status,
            String assignee,
            Pageable pageable) {
        Page<Task> tasks;
        if (status != null && assignee != null && !assignee.isBlank()) {
            tasks =
                    repository.findAllByStatusAndAssigneeUsernameIgnoreCase(
                            status, assignee, pageable);
        } else if (status != null) {
            tasks = repository.findAllByStatus(status, pageable);
        } else if (assignee != null && !assignee.isBlank()) {
            tasks = repository.findAllByAssigneeUsernameIgnoreCase(assignee, pageable);
        } else {
            tasks = repository.findAll(pageable);
        }
        return tasks.map(mapper::toResponse).getContent();
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
        if (request.creatorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "creatorId is required");
        }
        Task task = mapper.toEntity(request);
        task.setStatus(request.status() == null ? Status.TODO : request.status());
        task.setProject(findProject(request.projectId()));
        task.setCreator(findUser(request.creatorId()));
        task.setAssignee(request.assigneeId() == null ? null : findUser(request.assigneeId()));
        task.setUpdatedAt(Instant.now());
        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        invalidateSearchCache();
        return response;
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task oldTask = findTask(id);
        Task newTask = mapper.toEntity(request);
        newTask.setStatus(request.status() == null ? Status.TODO : request.status());
        newTask.setId(oldTask.getId());
        newTask.setCreatedAt(oldTask.getCreatedAt());
        newTask.setCreator(oldTask.getCreator());
        newTask.setProject(findProject(request.projectId()));
        newTask.setAssignee(request.assigneeId() == null ? null : findUser(request.assigneeId()));
        TaskResponse response = mapper.toResponse(repository.save(newTask));
        projectCache.invalidate();
        tagCache.invalidate();
        invalidateSearchCache();
        return response;
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
        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        invalidateSearchCache();
        return response;
    }

    public TaskResponse deleteTask(Long id) {
        Task task = findTask(id);
        repository.delete(task);
        TaskResponse response = mapper.toResponse(task);
        projectCache.invalidate();
        tagCache.invalidate();
        commentCache.invalidateTask(id);
        invalidateSearchCache();
        return response;
    }

    public List<TaskResponse> searchTasksByProjectIdAndTag(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            Pageable pageable) {
        TaskSearchKey key =
                TaskSearchKey.from(projectId, tagName, status, assignee, null, pageable, false);
        return getCachedSearch(
                key,
                () ->
                        repository.searchByProjectIdAndTag(
                                projectId, tagName, status, assignee, pageable));
    }

    public List<TaskResponse> searchOverdueTasksByProjectIdAndTagNative(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            Instant dueBefore,
            Pageable pageable) {
        Instant effectiveDueBefore = dueBefore == null ? Instant.now() : dueBefore;
        TaskSearchKey key = TaskSearchKey.from(
                        projectId, tagName, status, assignee, effectiveDueBefore, pageable, true);
        return getCachedSearch(
                key,
                () ->
                        repository.searchOverdueByProjectIdAndTagNative(
                                projectId, tagName, status, assignee, effectiveDueBefore, pageable));
    }

    private List<TaskResponse> getCachedSearch(
            TaskSearchKey key, Supplier<Page<Task>> loader) {
        List<TaskResponse> cached = searchCache.get(key);
        if (cached != null) {
            return cached;
        }
        Page<TaskResponse> page = loader.get().map(mapper::toResponse);
        List<TaskResponse> content = page.getContent();
        searchCache.put(key, content);
        return content;
    }

    private void invalidateSearchCache() {
        searchCache.invalidate();
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

}
