package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.cache.TaskCache.TaskQueryKey;
import com.ykleyka.taskboard.cache.TaskCache.TaskQueryType;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class TaskService {
    private static final Sort SEARCH_TASKS_SORT = Sort.by(Sort.Order.asc("id"));
    private static final Sort OVERDUE_TASKS_SORT =
            Sort.by(Sort.Order.asc("due_date"), Sort.Order.asc("id"));

    private final TaskMapper mapper;
    private final TaskRepository repository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectCache projectCache;
    private final TagCache tagCache;
    private final CommentCache commentCache;
    private final TaskCache taskCache;

    public List<TaskResponse> getTasks(
            Status status,
            String assignee,
            Long currentUserId,
            Pageable pageable) {
        String normalizedAssignee = normalizeAssigneeValue(assignee);
        Page<Task> tasks =
                repository.findAllVisibleToUser(currentUserId, status, normalizedAssignee, pageable);
        return refreshOverdueFlags(toTaskResponses(tasks));
    }

    public List<TaskResponse> getTasks(
            Status status,
            String assignee,
            Pageable pageable) {
        String normalizedAssignee = normalizeAssigneeValue(assignee);
        TaskQueryKey key =
                TaskQueryKey.from(
                        TaskQueryType.LIST,
                        null,
                        null,
                        status,
                        normalizedAssignee,
                        null,
                        pageable);
        return getCachedTasks(
                key,
                () -> {
                    if (status != null && normalizedAssignee != null) {
                        return repository.findAllByStatusAndAssigneeUsernameIgnoreCase(
                                status, normalizedAssignee, pageable);
                    }
                    if (status != null) {
                        return repository.findAllByStatus(status, pageable);
                    }
                    if (normalizedAssignee != null) {
                        return repository.findAllByAssigneeUsernameIgnoreCase(
                                normalizedAssignee, pageable);
                    }
                    return repository.findAll(pageable);
                });
    }

    public TaskDetailsResponse getTaskById(Long id, Long currentUserId) {
        TaskDetailsResponse cached = taskCache.getTaskDetails(id);
        if (cached != null) {
            requireProjectMember(cached.projectId(), currentUserId);
            log.info("Task {} details returned from cache", id);
            return cached;
        }
        Task task = findDetailedTask(id);
        requireTaskMember(task, currentUserId);
        TaskDetailsResponse response = mapper.toDetailsResponse(task);
        taskCache.putTaskDetails(id, response);
        return response;
    }

    public TaskDetailsResponse getTaskById(Long id) {
        TaskDetailsResponse cached = taskCache.getTaskDetails(id);
        if (cached != null) {
            log.info("Task {} details returned from cache", id);
            return cached;
        }
        TaskDetailsResponse response = mapper.toDetailsResponse(findDetailedTask(id));
        taskCache.putTaskDetails(id, response);
        return response;
    }

    private Task findTask(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Task findDetailedTask(Long id) {
        return repository.findDetailedById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public TaskResponse createTask(TaskRequest request, Long currentUserId) {
        Project project = findProject(request.projectId());
        requireProjectMember(project.getId(), currentUserId);
        User creator = findUser(currentUserId);
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());
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
        taskCache.invalidateQueries();
        return response;
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
        taskCache.invalidateQueries();
        return response;
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, Long currentUserId) {
        Task task = findTask(id);
        requireTaskMember(task, currentUserId);
        Project project = findProject(request.projectId());
        requireProjectMember(project.getId(), currentUserId);
        User creator = request.creatorId() == null ? task.getCreator() : findUser(request.creatorId());
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());
        if (creator != null) {
            ensureProjectMember(project.getId(), creator.getId());
        }
        if (assignee != null) {
            ensureProjectMember(project.getId(), assignee.getId());
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? Status.TODO : request.status());
        task.setPriority(request.priority());
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(Instant.now());

        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        taskCache.invalidateTask(id);
        return response;
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        Project project = findProject(request.projectId());
        User creator = request.creatorId() == null ? task.getCreator() : findUser(request.creatorId());
        User assignee = request.assigneeId() == null ? null : findUser(request.assigneeId());
        if (creator != null) {
            ensureProjectMember(project.getId(), creator.getId());
        }
        if (assignee != null) {
            ensureProjectMember(project.getId(), assignee.getId());
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() == null ? Status.TODO : request.status());
        task.setPriority(request.priority());
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(Instant.now());

        TaskResponse response = mapper.toResponse(repository.save(task));
        projectCache.invalidate();
        tagCache.invalidate();
        taskCache.invalidateTask(id);
        return response;
    }

    public TaskResponse patchTask(Long id, TaskPatchRequest request, Long currentUserId) {
        Task task = findTask(id);
        requireTaskMember(task, currentUserId);
        Project effectiveProject =
                request.projectId() == null ? task.getProject() : findProject(request.projectId());
        if (request.projectId() != null && effectiveProject != null) {
            requireProjectMember(effectiveProject.getId(), currentUserId);
        }
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
        taskCache.invalidateTask(id);
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
        taskCache.invalidateTask(id);
        return response;
    }

    public TaskResponse deleteTask(Long id, Long currentUserId) {
        Task task = findTask(id);
        requireTaskMember(task, currentUserId);
        return deleteTask(id);
    }

    public TaskResponse deleteTask(Long id) {
        Task task = findTask(id);
        repository.delete(task);
        TaskResponse response = mapper.toResponse(task);
        projectCache.invalidate();
        tagCache.invalidate();
        commentCache.invalidateTask(id);
        taskCache.invalidateTask(id);
        return response;
    }

    public List<TaskResponse> searchTasksByProjectIdAndTag(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            int page,
            int size,
            Long currentUserId) {
        requireProjectMember(projectId, currentUserId);
        return searchTasksByProjectIdAndTag(projectId, tagName, status, assignee, page, size);
    }

    public List<TaskResponse> searchTasksByProjectIdAndTag(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            int page,
            int size) {
        String normalizedTagName = normalizeTagName(tagName);
        String assigneePattern = normalizeAssigneePattern(assignee);
        Pageable pageable = PageRequest.of(page, size, SEARCH_TASKS_SORT);
        TaskQueryKey key =
                TaskQueryKey.from(
                        TaskQueryType.SEARCH,
                        projectId,
                        normalizedTagName,
                        status,
                        assigneePattern,
                        null,
                        pageable);
        return getCachedTasks(
                key,
                () ->
                        repository.searchByProjectIdAndTag(
                                projectId,
                                normalizedTagName,
                                status,
                                assigneePattern,
                                pageable));
    }

    public List<TaskResponse> searchOverdueTasksByProjectIdAndTagNative(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            Instant dueBefore,
            int page,
            int size,
            Long currentUserId) {
        requireProjectMember(projectId, currentUserId);
        return searchOverdueTasksByProjectIdAndTagNative(
                projectId,
                tagName,
                status,
                assignee,
                dueBefore,
                page,
                size);
    }

    public List<TaskResponse> searchOverdueTasksByProjectIdAndTagNative(
            Long projectId,
            String tagName,
            Status status,
            String assignee,
            Instant dueBefore,
            int page,
            int size) {
        String normalizedTagName = normalizeTagName(tagName);
        String statusValue = normalizeStatusValue(status);
        String assigneePattern = normalizeAssigneePattern(assignee);
        Pageable nativePageable = PageRequest.of(page, size, OVERDUE_TASKS_SORT);
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
        TaskQueryKey key =
                TaskQueryKey.from(
                        TaskQueryType.OVERDUE,
                        projectId,
                        normalizedTagName,
                        status,
                        assigneePattern,
                        effectiveDueBefore,
                        nativePageable);
        return getCachedTasks(key, loader);
    }

    private List<TaskResponse> getCachedTasks(
            TaskQueryKey key, Supplier<Page<Task>> loader) {
        List<TaskResponse> cached = taskCache.getQuery(key);
        if (cached != null) {
            log.info(
                    "Task query returned from cache: type={}, projectId={}, tagName={}, status={}, " +
                            "assignee={}, dueBefore={}, page={}, size={}, sort={}",
                    key.getQueryType(),
                    key.getProjectId(),
                    key.getTagName(),
                    key.getStatus(),
                    key.getAssignee(),
                    key.getDueBefore(),
                    key.getPage(),
                    key.getSize(),
                    key.getSort());
            return refreshOverdueFlags(cached);
        }
        List<TaskResponse> content = toTaskResponses(loader.get());
        taskCache.putQuery(key, content);
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

    private String normalizeTagName(String tagName) {
        return tagName == null ? null : tagName.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeAssigneeValue(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        return assignee.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeAssigneePattern(String assignee) {
        String normalizedAssignee = normalizeAssigneeValue(assignee);
        if (normalizedAssignee == null) {
            return null;
        }
        return "%" + normalizedAssignee + "%";
    }

    private String normalizeStatusValue(Status status) {
        return status == null ? null : status.name();
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    public void requireTaskMember(Long taskId, Long userId) {
        requireTaskMember(findTask(taskId), userId);
    }

    public void requireTaskEditor(Long taskId, Long userId) {
        requireTaskMember(findTask(taskId), userId);
    }

    private void requireTaskMember(Task task, Long userId) {
        requireProjectMember(requireTaskProjectId(task), userId);
    }

    private void requireProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
    }

    private Long requireTaskProjectId(Task task) {
        if (task == null || task.getProject() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task has no project");
        }
        return task.getProject().getId();
    }

    private void ensureProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "user must be a member of project " + projectId);
        }
    }

}
