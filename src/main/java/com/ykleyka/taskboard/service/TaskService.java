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
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper mapper;
    private final TaskRepository repository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
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
        Project project = findProject(request.projectId());
        User creator = findUser(request.creatorId());
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());
        ensureProjectMember(project.getId(), creator.getId());
        if (assignee != null) {
            ensureProjectMember(project.getId(), assignee.getId());
        }
        Task task = mapper.toEntity(request);
        task.setStatus(request.status() == null ? Status.TODO : request.status());
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setUpdatedAt(Instant.now());
        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        invalidateSearchCache();
        return response;
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        Project project = findProject(request.projectId());
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());
        User creator = task.getCreator();
        if (creator != null) {
            ensureProjectMember(project.getId(), creator.getId());
        }
        if (assignee != null) {
            ensureProjectMember(project.getId(), assignee.getId());
        }

        Status effectiveStatus = request.status() == null ? task.getStatus() : request.status();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(effectiveStatus == null ? Status.TODO : effectiveStatus);
        task.setPriority(request.priority());
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(Instant.now());

        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        invalidateSearchCache();
        return response;
    }

    public TaskResponse patchTask(Long id, TaskPatchRequest request) {
        Task task = findTask(id);
        Project effectiveProject =
                request.projectId() == null ? task.getProject() : findProject(request.projectId());
        User effectiveAssignee =
                request.assigneeId() == null ? task.getAssignee() : findUser(request.assigneeId());
        User creator = task.getCreator();
        if (effectiveProject != null) {
            if (creator != null) {
                ensureProjectMember(effectiveProject.getId(), creator.getId());
            }
            if (effectiveAssignee != null) {
                ensureProjectMember(effectiveProject.getId(), effectiveAssignee.getId());
            }
        }

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
            task.setProject(effectiveProject);
        }
        if (request.assigneeId() != null) {
            task.setAssignee(effectiveAssignee);
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
        String normalizedTagName = normalizeTagName(tagName);
        String assigneePattern = normalizeAssigneePattern(assignee);
        TaskSearchKey key =
                TaskSearchKey.from(
                        projectId, normalizedTagName, status, assigneePattern, null, pageable, false);
        return getCachedSearch(
                key,
                () ->
                        repository.searchByProjectIdAndTag(
                                projectId, normalizedTagName, status, assigneePattern, pageable));
    }

    public List<TaskResponse> searchOverdueTasksByProjectIdAndTagNative(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            Instant dueBefore,
            Pageable pageable) {
        String normalizedTagName = normalizeTagName(tagName);
        String statusValue = normalizeStatusValue(status);
        String assigneePattern = normalizeAssigneePattern(assignee);
        Pageable nativePageable = normalizeNativePageable(pageable);
        Instant effectiveDueBefore = dueBefore == null ? Instant.now() : dueBefore;
        Supplier<Page<Task>> loader =
                () ->
                        repository.searchOverdueByProjectIdAndTagNative(
                                projectId,
                                normalizedTagName,
                                statusValue,
                                assigneePattern,
                                effectiveDueBefore,
                                nativePageable);
        if (dueBefore == null) {
            return toTaskResponses(loader.get());
        }
        TaskSearchKey key =
                TaskSearchKey.from(
                        projectId,
                        normalizedTagName,
                        status,
                        assigneePattern,
                        effectiveDueBefore,
                        nativePageable,
                        true);
        return getCachedSearch(key, loader);
    }

    private List<TaskResponse> getCachedSearch(
            TaskSearchKey key, Supplier<Page<Task>> loader) {
        List<TaskResponse> cached = searchCache.get(key);
        if (cached != null) {
            return refreshOverdueFlags(cached);
        }
        List<TaskResponse> content = toTaskResponses(loader.get());
        searchCache.put(key, content);
        return refreshOverdueFlags(content);
    }

    private List<TaskResponse> toTaskResponses(Page<Task> page) {
        return page.map(mapper::toResponse).getContent();
    }

    private List<TaskResponse> refreshOverdueFlags(List<TaskResponse> responses) {
        Instant now = Instant.now();
        return responses.stream()
                .map(response -> refreshOverdueFlag(response, now))
                .toList();
    }

    private TaskResponse refreshOverdueFlag(TaskResponse response, Instant now) {
        boolean overdue =
                response.dueDate() != null
                        && response.dueDate().isBefore(now)
                        && response.status() != Status.COMPLETED;
        return new TaskResponse(
                response.id(),
                response.title(),
                response.description(),
                response.status(),
                response.priority(),
                response.projectId(),
                response.projectName(),
                response.creatorId(),
                response.creatorUsername(),
                response.assigneeId(),
                response.assigneeUsername(),
                response.dueDate(),
                overdue,
                response.createdAt(),
                response.updatedAt());
    }

    private void invalidateSearchCache() {
        searchCache.invalidate();
    }

    private String normalizeTagName(String tagName) {
        return tagName == null ? null : tagName.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeAssigneePattern(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        return "%" + assignee.strip().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeStatusValue(Status status) {
        return status == null ? null : status.name();
    }

    private Pageable normalizeNativePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return pageable;
        }
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        Sort normalizedSort = Sort.by(
                pageable.getSort().stream()
                        .map(this::normalizeNativeOrder)
                        .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private Sort.Order normalizeNativeOrder(Sort.Order order) {
        String column = switch (order.getProperty()) {
            case "dueDate" -> "due_date";
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "projectId" -> "project_id";
            case "creatorId" -> "creator_id";
            case "assigneeId" -> "assignee_id";
            case "assigneeUsername" -> "a.username";
            default -> order.getProperty();
        };
        Sort.Order normalized = new Sort.Order(order.getDirection(), column);
        if (order.isIgnoreCase()) {
            normalized = normalized.ignoreCase();
        }
        return switch (order.getNullHandling()) {
            case NULLS_FIRST -> normalized.nullsFirst();
            case NULLS_LAST -> normalized.nullsLast();
            default -> normalized;
        };
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private void ensureProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "user must be a member of project " + projectId);
        }
    }

}
