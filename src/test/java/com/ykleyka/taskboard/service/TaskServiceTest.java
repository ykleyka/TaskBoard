package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskPatchRequest;
import com.ykleyka.taskboard.dto.TaskRequest;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.TaskMapper;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.Priority;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.model.enums.Status;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskMapper mapper;
    @Mock
    private TaskRepository repository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectCache projectCache;
    @Mock
    private TagCache tagCache;
    @Mock
    private CommentCache commentCache;
    @Mock
    private TaskCache taskCache;

    @InjectMocks
    private TaskService service;

    @Test
    void getTasks_whenCacheHit_returnsCachedAndRefreshesOverdueFlags() {
        Pageable pageable = PageRequest.of(0, 20);
        TaskResponse overdueCandidate =
                taskResponse(
                        1L,
                        "Overdue",
                        Status.TODO,
                        Instant.now().minusSeconds(3600),
                        false);
        TaskResponse completedCandidate =
                taskResponse(
                        2L,
                        "Completed",
                        Status.COMPLETED,
                        Instant.now().minusSeconds(3600),
                        true);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class)))
                .thenReturn(List.of(overdueCandidate, completedCandidate));

        List<TaskResponse> result = service.getTasks(null, null, pageable);

        assertEquals(2, result.size());
        assertTrue(result.get(0).overdue());
        assertFalse(result.get(1).overdue());
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getTasks_whenCacheHit_hasFutureDueDate_keepsOverdueFalse() {
        Pageable pageable = PageRequest.of(0, 20);
        TaskResponse futureCandidate =
                taskResponse(
                        5L,
                        "Future",
                        Status.IN_PROGRESS,
                        Instant.now().plusSeconds(3600),
                        true);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(List.of(futureCandidate));

        List<TaskResponse> result = service.getTasks(null, null, pageable);

        assertEquals(1, result.size());
        assertFalse(result.get(0).overdue());
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getTasks_whenStatusAndAssigneePresent_usesNormalizedAssignee() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = task(1L, project(1L), user(10L, "creator"), user(20L, "john"));
        TaskResponse mapped = taskResponse(1L, "Mapped", Status.IN_PROGRESS, null, false);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.findAllByStatusAndAssigneeUsernameIgnoreCase(Status.IN_PROGRESS, "john", pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result = service.getTasks(Status.IN_PROGRESS, "  JoHn  ", pageable);

        assertEquals(1, result.size());
        assertEquals("Mapped", result.get(0).title());
        verify(repository).findAllByStatusAndAssigneeUsernameIgnoreCase(Status.IN_PROGRESS, "john", pageable);
        verify(taskCache).putQuery(any(TaskCache.TaskQueryKey.class), any());
    }

    @Test
    void getTasks_whenOnlyStatusPresent_usesStatusRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = task(2L, project(1L), user(10L, "creator"), null);
        TaskResponse mapped = taskResponse(2L, "By status", Status.TODO, null, false);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.findAllByStatus(Status.TODO, pageable)).thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result = service.getTasks(Status.TODO, null, pageable);

        assertEquals(1, result.size());
        verify(repository).findAllByStatus(Status.TODO, pageable);
    }

    @Test
    void getTasks_whenOnlyAssigneePresent_usesAssigneeRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = task(3L, project(1L), user(10L, "creator"), user(20L, "bob"));
        TaskResponse mapped = taskResponse(3L, "By assignee", Status.TODO, null, false);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.findAllByAssigneeUsernameIgnoreCase("bob", pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result = service.getTasks(null, " BoB ", pageable);

        assertEquals(1, result.size());
        verify(repository).findAllByAssigneeUsernameIgnoreCase("bob", pageable);
    }

    @Test
    void getTasks_whenNoFiltersOrBlankAssignee_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Task task = task(4L, project(1L), user(10L, "creator"), null);
        TaskResponse mapped = taskResponse(4L, "All", Status.TODO, null, false);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result = service.getTasks(null, "   ", pageable);

        assertEquals(1, result.size());
        verify(repository).findAll(pageable);
    }

    @Test
    void getTasks_withCurrentUser_usesVisibleTasksAndNormalizedAssignee() {
        Pageable pageable = PageRequest.of(0, 20);
        Long currentUserId = 42L;
        Task task = task(6L, project(1L), user(10L, "creator"), user(20L, "alice"));
        TaskResponse mapped = taskResponse(6L, "Visible", Status.TODO, null, false);

        when(repository.findAllVisibleToUser(currentUserId, Status.TODO, "alice", pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result = service.getTasks(Status.TODO, " ALICE ", currentUserId, pageable);

        assertEquals(1, result.size());
        assertEquals("Visible", result.get(0).title());
        verify(repository).findAllVisibleToUser(currentUserId, Status.TODO, "alice", pageable);
    }

    @Test
    void getTaskById_whenCacheHit_returnsCached() {
        TaskDetailsResponse cached = taskDetails(1L, "Cached");
        when(taskCache.getTaskDetails(1L)).thenReturn(cached);

        TaskDetailsResponse actual = service.getTaskById(1L);

        assertEquals(cached, actual);
        verify(repository, never()).findDetailedById(any());
    }

    @Test
    void getTaskById_whenCacheMiss_loadsAndCaches() {
        Task task = task(2L, project(1L), user(10L, "creator"), null);
        TaskDetailsResponse mapped = taskDetails(2L, "Details");

        when(taskCache.getTaskDetails(2L)).thenReturn(null);
        when(repository.findDetailedById(2L)).thenReturn(Optional.of(task));
        when(mapper.toDetailsResponse(task)).thenReturn(mapped);

        TaskDetailsResponse actual = service.getTaskById(2L);

        assertEquals(mapped, actual);
        verify(taskCache).putTaskDetails(2L, mapped);
    }

    @Test
    void getTaskById_whenMissing_throwsTaskNotFound() {
        when(taskCache.getTaskDetails(3L)).thenReturn(null);
        when(repository.findDetailedById(3L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.getTaskById(3L));
    }

    @Test
    void getTaskById_withCurrentUserAndCachedTask_requiresProjectMembership() {
        Long taskId = 4L;
        Long currentUserId = 44L;
        TaskDetailsResponse cached = taskDetails(taskId, "Cached");

        when(taskCache.getTaskDetails(taskId)).thenReturn(cached);
        when(projectMemberRepository.existsByProjectIdAndUserId(cached.projectId(), currentUserId))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.getTaskById(taskId, currentUserId));

        assertEquals(404, exception.getStatusCode().value());
        verify(repository, never()).findDetailedById(any());
    }

    @Test
    void getTaskById_withCurrentUserAndCacheMiss_loadsDetailsWhenMember() {
        Long taskId = 5L;
        Long projectId = 6L;
        Long currentUserId = 7L;
        Task task = task(taskId, project(projectId), user(10L, "creator"), null);
        TaskDetailsResponse mapped = taskDetails(taskId, "Member details");

        when(taskCache.getTaskDetails(taskId)).thenReturn(null);
        when(repository.findDetailedById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(mapper.toDetailsResponse(task)).thenReturn(mapped);

        TaskDetailsResponse actual = service.getTaskById(taskId, currentUserId);

        assertEquals(mapped, actual);
        verify(taskCache).putTaskDetails(taskId, mapped);
    }

    @Test
    void createTask_whenCreatorIdIsMissing_throwsBadRequest() {
        TaskRequest request =
                new TaskRequest("Task", "Description", 1L, null, null, null, Priority.MEDIUM, null);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createTask(request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createTask_whenProjectMissing_throwsProjectNotFound() {
        TaskRequest request =
                new TaskRequest("Task", "Description", 1L, 2L, null, null, Priority.MEDIUM, null);
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.createTask(request));
    }

    @Test
    void createTask_whenCreatorMissing_throwsUserNotFound() {
        TaskRequest request =
                new TaskRequest("Task", "Description", 1L, 2L, null, null, Priority.MEDIUM, null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project(1L)));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.createTask(request));
    }

    @Test
    void createTask_whenCreatorNotMember_throwsBadRequest() {
        Long projectId = 1L;
        Long creatorId = 2L;
        TaskRequest request =
                new TaskRequest("Task", "Description", projectId, creatorId, null, null, Priority.HIGH, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, "creator")));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createTask(request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createTask_whenAssigneeNotMember_throwsBadRequest() {
        Long projectId = 1L;
        Long creatorId = 2L;
        Long assigneeId = 3L;
        TaskRequest request =
                new TaskRequest(
                        "Task",
                        "Description",
                        projectId,
                        creatorId,
                        assigneeId,
                        null,
                        Priority.HIGH,
                        null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, "creator")));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId, "assignee")));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createTask(request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createTask_whenAssigneeIsMember_savesWithAssignee() {
        Long projectId = 1L;
        Long creatorId = 2L;
        Long assigneeId = 3L;
        TaskRequest request =
                new TaskRequest(
                        "Task",
                        "Description",
                        projectId,
                        creatorId,
                        assigneeId,
                        null,
                        Priority.HIGH,
                        Instant.parse("2030-01-01T00:00:00Z"));
        Task mappedTask = new Task();
        User creator = user(creatorId, "creator");
        User assignee = user(assigneeId, "assignee");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(true);
        when(mapper.toEntity(request)).thenReturn(mappedTask);
        when(repository.save(mappedTask)).thenReturn(mappedTask);
        when(mapper.toResponse(mappedTask)).thenReturn(taskResponse(102L, "Task", Status.TODO, null, false));

        service.createTask(request);

        assertEquals(assignee, mappedTask.getAssignee());
        assertEquals(creator, mappedTask.getCreator());
    }

    @Test
    void createTask_whenValidWithoutAssignee_setsDefaultStatusAndInvalidates() {
        Long projectId = 1L;
        Long creatorId = 2L;
        TaskRequest request =
                new TaskRequest(
                        "Task",
                        "Description",
                        projectId,
                        creatorId,
                        null,
                        null,
                        Priority.HIGH,
                        Instant.parse("2030-01-01T00:00:00Z"));
        Task mappedTask = new Task();
        TaskResponse expected = taskResponse(100L, "Task", Status.TODO, null, false);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, "creator")));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(true);
        when(mapper.toEntity(request)).thenReturn(mappedTask);
        when(repository.save(mappedTask)).thenReturn(mappedTask);
        when(mapper.toResponse(mappedTask)).thenReturn(expected);

        TaskResponse actual = service.createTask(request);

        assertEquals(expected, actual);
        assertEquals(Status.TODO, mappedTask.getStatus());
        assertNotNull(mappedTask.getUpdatedAt());
        verify(projectCache).invalidate();
        verify(tagCache).invalidate();
        verify(taskCache).invalidateQueries();
    }

    @Test
    void createTask_whenStatusProvided_keepsProvidedStatus() {
        Long projectId = 1L;
        Long creatorId = 2L;
        TaskRequest request =
                new TaskRequest(
                        "Task",
                        "Description",
                        projectId,
                        creatorId,
                        null,
                        Status.IN_PROGRESS,
                        Priority.HIGH,
                        null);
        Task mappedTask = new Task();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(user(creatorId, "creator")));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(true);
        when(mapper.toEntity(request)).thenReturn(mappedTask);
        when(repository.save(mappedTask)).thenReturn(mappedTask);
        when(mapper.toResponse(mappedTask))
                .thenReturn(taskResponse(101L, "Task", Status.IN_PROGRESS, null, false));

        service.createTask(request);

        assertEquals(Status.IN_PROGRESS, mappedTask.getStatus());
    }

    @Test
    void createTask_withCurrentUser_usesCurrentUserAsCreator() {
        Long projectId = 1L;
        Long currentUserId = 2L;
        Long assigneeId = 3L;
        TaskRequest request =
                new TaskRequest(
                        "Task",
                        "Description",
                        projectId,
                        null,
                        assigneeId,
                        Status.IN_PROGRESS,
                        Priority.HIGH,
                        null);
        Task mappedTask = new Task();
        User currentUser = user(currentUserId, "current");
        User assignee = user(assigneeId, "assignee");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId))
                .thenReturn(true);
        when(mapper.toEntity(request)).thenReturn(mappedTask);
        when(repository.save(mappedTask)).thenReturn(mappedTask);
        when(mapper.toResponse(mappedTask))
                .thenReturn(taskResponse(103L, "Task", Status.IN_PROGRESS, null, false));

        service.createTask(request, currentUserId);

        assertEquals(currentUser, mappedTask.getCreator());
        assertEquals(assignee, mappedTask.getAssignee());
        assertEquals(Status.IN_PROGRESS, mappedTask.getStatus());
    }

    @Test
    void updateTask_whenCreatorFromTaskAndAssigneeNull_updatesTask() {
        Long taskId = 10L;
        Long projectId = 1L;
        User creator = user(2L, "creator");
        Task existing = task(taskId, project(projectId), creator, user(3L, "old-assignee"));
        TaskRequest request =
                new TaskRequest(
                        "Updated",
                        "Updated desc",
                        projectId,
                        null,
                        null,
                        null,
                        Priority.LOW,
                        Instant.parse("2030-01-01T00:00:00Z"));

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creator.getId())).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "Updated", Status.TODO, null, false));

        TaskResponse actual = service.updateTask(taskId, request);

        assertEquals("Updated", existing.getTitle());
        assertEquals("Updated desc", existing.getDescription());
        assertEquals(Status.TODO, existing.getStatus());
        assertEquals(Priority.LOW, existing.getPriority());
        assertNull(existing.getAssignee());
        assertEquals(actual.id(), taskId);
        verify(taskCache).invalidateTask(taskId);
    }

    @Test
    void updateTask_whenCreatorAndAssigneeProvided_keepsOriginalCreator() {
        Long taskId = 11L;
        Long projectId = 1L;
        Long creatorId = 2L;
        Long assigneeId = 3L;
        User originalCreator = user(9L, "old");
        Task existing = task(taskId, project(projectId), originalCreator, null);
        TaskRequest request =
                new TaskRequest(
                        "Updated",
                        "Desc",
                        projectId,
                        creatorId,
                        assigneeId,
                        Status.IN_PROGRESS,
                        Priority.HIGH,
                        null);

        User assignee = user(assigneeId, "assignee");

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, originalCreator.getId()))
                .thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing))
                .thenReturn(taskResponse(taskId, "Updated", Status.IN_PROGRESS, null, false));

        service.updateTask(taskId, request);

        assertEquals(originalCreator, existing.getCreator());
        assertEquals(assignee, existing.getAssignee());
        assertEquals(Status.IN_PROGRESS, existing.getStatus());
    }

    @Test
    void updateTask_whenTaskCreatorIsNull_skipsCreatorMembershipCheck() {
        Long taskId = 12L;
        Long projectId = 1L;
        Task existing = task(taskId, project(projectId), null, null);
        TaskRequest request =
                new TaskRequest("Title", "Desc", projectId, null, null, null, Priority.MEDIUM, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "Title", Status.TODO, null, false));

        service.updateTask(taskId, request);

        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(eq(projectId), any());
    }

    @Test
    void updateTask_whenProvidedCreatorMissing_ignoresCreatorId() {
        Long taskId = 13L;
        Long projectId = 1L;
        Long creatorId = 99L;
        User originalCreator = user(2L, "creator");
        Task existing = task(taskId, project(projectId), originalCreator, null);
        TaskRequest request =
                new TaskRequest("Title", "Desc", projectId, creatorId, null, null, Priority.MEDIUM, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, originalCreator.getId()))
                .thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "Title", Status.TODO, null, false));

        service.updateTask(taskId, request);

        assertEquals(originalCreator, existing.getCreator());
        verify(userRepository, never()).findById(creatorId);
    }

    @Test
    void updateTask_withCurrentUser_checksOldAndNewProjectMembership() {
        Long taskId = 14L;
        Long oldProjectId = 1L;
        Long newProjectId = 2L;
        Long currentUserId = 3L;
        User creator = user(currentUserId, "creator");
        Task existing = task(taskId, project(oldProjectId), creator, null);
        TaskRequest request =
                new TaskRequest(
                        "Updated",
                        "Desc",
                        newProjectId,
                        null,
                        null,
                        Status.IN_PROGRESS,
                        Priority.HIGH,
                        null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.existsByProjectIdAndUserId(oldProjectId, currentUserId))
                .thenReturn(true);
        when(projectMemberRepository.findById(any()))
                .thenReturn(Optional.of(projectMember(oldProjectId, creator, ProjectRole.MEMBER)));
        when(projectRepository.findById(newProjectId)).thenReturn(Optional.of(project(newProjectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(newProjectId, currentUserId))
                .thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing))
                .thenReturn(taskResponse(taskId, "Updated", Status.IN_PROGRESS, null, false));

        service.updateTask(taskId, request, currentUserId);

        assertEquals(newProjectId, existing.getProject().getId());
        assertEquals(Status.IN_PROGRESS, existing.getStatus());
        verify(projectMemberRepository).existsByProjectIdAndUserId(oldProjectId, currentUserId);
        verify(projectMemberRepository, org.mockito.Mockito.atLeastOnce())
                .existsByProjectIdAndUserId(newProjectId, currentUserId);
    }

    @Test
    void patchTask_whenEffectiveProjectIsNull_skipsMembershipCheckAndUpdatesOptionalFields() {
        Long taskId = 20L;
        Task existing = task(taskId, null, null, null);
        TaskPatchRequest request =
                new TaskPatchRequest(
                        "Patched title",
                        "Patched desc",
                        null,
                        null,
                        Status.IN_PROGRESS,
                        Priority.HIGH,
                        Instant.parse("2030-02-01T00:00:00Z"));

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing))
                .thenReturn(taskResponse(taskId, "Patched title", Status.IN_PROGRESS, request.dueDate(), false));

        service.patchTask(taskId, request);

        assertEquals("Patched title", existing.getTitle());
        assertEquals("Patched desc", existing.getDescription());
        assertEquals(Status.IN_PROGRESS, existing.getStatus());
        assertEquals(Priority.HIGH, existing.getPriority());
        assertEquals(request.dueDate(), existing.getDueDate());
    }

    @Test
    void patchTask_whenProjectAndAssigneeProvided_checksMembershipAndAppliesChanges() {
        Long taskId = 21L;
        Long projectId = 4L;
        Long assigneeId = 5L;
        User creator = user(2L, "creator");
        User oldAssignee = user(3L, "old-assignee");
        Task existing = task(taskId, project(1L), creator, oldAssignee);
        User newAssignee = user(assigneeId, "new-assignee");
        TaskPatchRequest request =
                new TaskPatchRequest(
                        null,
                        null,
                        projectId,
                        assigneeId,
                        null,
                        null,
                        null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(newAssignee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creator.getId())).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "x", Status.TODO, null, false));

        service.patchTask(taskId, request);

        assertEquals(projectId, existing.getProject().getId());
        assertEquals(newAssignee, existing.getAssignee());
    }

    @Test
    void patchTask_whenAssigneeProvidedButMissing_throwsUserNotFound() {
        Long taskId = 25L;
        Long assigneeId = 77L;
        Task existing = task(taskId, project(1L), user(2L, "creator"), null);
        TaskPatchRequest request = new TaskPatchRequest(null, null, null, assigneeId, null, null, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.patchTask(taskId, request));
    }

    @Test
    void patchTask_whenProjectProvidedAndCreatorNull_checksOnlyAssigneeMembership() {
        Long taskId = 23L;
        Long projectId = 6L;
        Long assigneeId = 7L;
        User assignee = user(assigneeId, "assignee");
        Task existing = task(taskId, project(1L), null, assignee);
        TaskPatchRequest request = new TaskPatchRequest(null, null, projectId, null, null, null, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "x", Status.TODO, null, false));

        service.patchTask(taskId, request);

        assertEquals(projectId, existing.getProject().getId());
        verify(projectMemberRepository).existsByProjectIdAndUserId(projectId, assigneeId);
        verifyNoMoreInteractions(projectMemberRepository);
    }

    @Test
    void patchTask_whenProjectProvidedAndEffectiveAssigneeNull_checksOnlyCreatorMembership() {
        Long taskId = 24L;
        Long projectId = 7L;
        Long creatorId = 2L;
        User creator = user(creatorId, "creator");
        Task existing = task(taskId, project(1L), creator, null);
        TaskPatchRequest request = new TaskPatchRequest(null, null, projectId, null, null, null, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creatorId)).thenReturn(true);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "x", Status.TODO, null, false));

        service.patchTask(taskId, request);

        assertEquals(projectId, existing.getProject().getId());
        verify(projectMemberRepository).existsByProjectIdAndUserId(projectId, creatorId);
        verifyNoMoreInteractions(projectMemberRepository);
    }

    @Test
    void patchTask_whenMembershipInvalid_throwsBadRequest() {
        Long taskId = 22L;
        Long projectId = 4L;
        User creator = user(2L, "creator");
        Task existing = task(taskId, project(1L), creator, null);
        TaskPatchRequest request = new TaskPatchRequest(null, null, projectId, null, null, null, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, creator.getId())).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.patchTask(taskId, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void patchTask_withCurrentProjectMember_updatesStatus() {
        Long taskId = 90L;
        Long projectId = 91L;
        Long currentUserId = 92L;
        Task existing = task(taskId, project(projectId), null, user(currentUserId, "assignee"));
        TaskPatchRequest request =
                new TaskPatchRequest(null, null, null, null, Status.COMPLETED, null, null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(projectMemberRepository.findById(any()))
                .thenReturn(Optional.of(projectMember(projectId, user(currentUserId, "assignee"), ProjectRole.MEMBER)));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing))
                .thenReturn(taskResponse(taskId, "Patched", Status.COMPLETED, null, false));

        TaskResponse actual = service.patchTask(taskId, request, currentUserId);

        assertEquals(taskId, actual.id());
        assertEquals(Status.COMPLETED, existing.getStatus());
        verify(projectMemberRepository, org.mockito.Mockito.atLeastOnce())
                .existsByProjectIdAndUserId(projectId, currentUserId);
    }

    @Test
    void patchTask_withAssigneeChangingDeadline_throwsForbidden() {
        Long taskId = 91L;
        Long projectId = 92L;
        Long assigneeId = 93L;
        User assignee = user(assigneeId, "assignee");
        Task existing = task(taskId, project(projectId), user(10L, "creator"), assignee);
        TaskPatchRequest request =
                new TaskPatchRequest(null, null, null, null, null, null, Instant.parse("2030-01-01T00:00:00Z"));

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId)).thenReturn(true);
        when(projectMemberRepository.findById(any()))
                .thenReturn(Optional.of(projectMember(projectId, assignee, ProjectRole.MEMBER)));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.patchTask(taskId, request, assigneeId));

        assertEquals(403, exception.getStatusCode().value());
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_withMemberWhoIsNotCreator_throwsForbidden() {
        Long taskId = 92L;
        Long projectId = 93L;
        Long currentUserId = 94L;
        User member = user(currentUserId, "member");
        Task existing = task(taskId, project(projectId), user(10L, "creator"), null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(projectMemberRepository.findById(any()))
                .thenReturn(Optional.of(projectMember(projectId, member, ProjectRole.MEMBER)));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.deleteTask(taskId, currentUserId));

        assertEquals(403, exception.getStatusCode().value());
        verify(repository, never()).delete(any(Task.class));
    }

    @Test
    void deleteTask_whenValid_deletesAndInvalidatesRelatedCaches() {
        Long taskId = 30L;
        Task existing = task(taskId, project(1L), user(2L, "creator"), null);
        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(taskResponse(taskId, "Deleted", Status.TODO, null, false));

        TaskResponse actual = service.deleteTask(taskId);

        assertEquals(taskId, actual.id());
        verify(repository).delete(existing);
        verify(projectCache).invalidate();
        verify(tagCache).invalidate();
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTask(taskId);
    }

    @Test
    void deleteTask_whenMissing_throwsTaskNotFound() {
        when(repository.findById(31L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.deleteTask(31L));
    }

    @Test
    void deleteTask_withCurrentUserAndMissingMembership_hidesTask() {
        Long taskId = 32L;
        Long projectId = 33L;
        Long currentUserId = 34L;
        Task existing = task(taskId, project(projectId), user(2L, "creator"), null);

        when(repository.findById(taskId)).thenReturn(Optional.of(existing));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.deleteTask(taskId, currentUserId));

        assertEquals(404, exception.getStatusCode().value());
        verify(repository, never()).delete(any(Task.class));
    }

    @Test
    void searchTasksByProjectIdAndTag_whenCacheMiss_usesNormalizedInputs() {
        Long projectId = 40L;
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("id"));
        Task task = task(41L, project(projectId), user(1L, "creator"), user(2L, "bob"));
        TaskResponse mapped = taskResponse(41L, "Search", Status.TODO, null, false);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.searchByProjectIdAndTag(
                projectId, "backend", Status.TODO, "%bob%", pageable))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result =
                service.searchTasksByProjectIdAndTag(
                        projectId,
                        "  BackEnd ",
                        Status.TODO,
                        " BoB ",
                        PageRequest.of(0, 20));

        assertEquals(1, result.size());
        verify(repository).searchByProjectIdAndTag(projectId, "backend", Status.TODO, "%bob%", pageable);
    }

    @Test
    void searchTasksByProjectIdAndTag_whenCacheHit_returnsCachedValue() {
        Long projectId = 41L;
        TaskResponse cached = taskResponse(1L, "Cached", Status.TODO, null, false);
        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(List.of(cached));

        List<TaskResponse> result =
                service.searchTasksByProjectIdAndTag(projectId, null, null, " ", PageRequest.of(0, 20));

        assertEquals(1, result.size());
        verify(repository, never()).searchByProjectIdAndTag(any(), any(), any(), any(), any());
    }

    @Test
    void searchOverdue_whenDueBeforeNull_bypassesCache() {
        Long projectId = 50L;
        Task task = task(51L, project(projectId), user(1L, "creator"), null);
        TaskResponse mapped = taskResponse(51L, "Overdue", Status.IN_PROGRESS, Instant.now().minusSeconds(100), true);

        when(repository.searchOverdueByProjectIdAndTagNative(
                eq(projectId),
                eq("tag"),
                eq(null),
                eq(null),
                any(Instant.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result =
                service.searchOverdueTasksByProjectIdAndTagNative(
                        projectId,
                        "TAG",
                        null,
                        " ",
                        null,
                        PageRequest.of(0, 20));

        assertEquals(1, result.size());
        verify(taskCache, never()).putQuery(any(), any());
    }

    @Test
    void searchOverdue_whenDueBeforeProvided_cacheMiss_usesCacheAndStatusValue() {
        Long projectId = 60L;
        Instant dueBefore = Instant.parse("2030-01-01T00:00:00Z");
        Task task = task(61L, project(projectId), user(1L, "creator"), user(2L, "alex"));
        TaskResponse mapped = taskResponse(61L, "Overdue", Status.IN_PROGRESS, dueBefore.minusSeconds(60), true);

        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(null);
        when(repository.searchOverdueByProjectIdAndTagNative(
                projectId,
                "tag",
                "IN_PROGRESS",
                "%alex%",
                dueBefore,
                PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.asc("due_date"),
                        org.springframework.data.domain.Sort.Order.asc("id")))))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(mapper.toResponse(task)).thenReturn(mapped);

        List<TaskResponse> result =
                service.searchOverdueTasksByProjectIdAndTagNative(
                        projectId,
                        "TAG",
                        Status.IN_PROGRESS,
                        " Alex ",
                        dueBefore,
                        PageRequest.of(0, 20));

        assertEquals(1, result.size());
        verify(taskCache).putQuery(any(TaskCache.TaskQueryKey.class), any());
    }

    @Test
    void searchOverdue_whenDueBeforeProvided_cacheHit_returnsCached() {
        Long projectId = 70L;
        Instant dueBefore = Instant.parse("2031-01-01T00:00:00Z");
        TaskResponse cached = taskResponse(71L, "Cached overdue", Status.TODO, dueBefore.minusSeconds(1), false);
        when(taskCache.getQuery(any(TaskCache.TaskQueryKey.class))).thenReturn(List.of(cached));

        List<TaskResponse> result =
                service.searchOverdueTasksByProjectIdAndTagNative(
                        projectId,
                        "Tag",
                        Status.TODO,
                        "bob",
                        dueBefore,
                        PageRequest.of(0, 20));

        assertEquals(1, result.size());
        verify(repository, never()).searchOverdueByProjectIdAndTagNative(
                any(), any(), any(), any(), any(), any());
    }

    private Task task(Long id, Project project, User creator, User assignee) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Task-" + id);
        task.setDescription("description");
        task.setStatus(Status.TODO);
        task.setPriority(Priority.MEDIUM);
        task.setProject(project);
        task.setCreator(creator);
        task.setAssignee(assignee);
        task.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        task.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return task;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("Project-" + id);
        return project;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private ProjectMember projectMember(Long projectId, User user, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setProject(project(projectId));
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private TaskResponse taskResponse(
            Long id,
            String title,
            Status status,
            Instant dueDate,
            boolean overdue) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new TaskResponse(
                id,
                title,
                "description",
                status,
                Priority.MEDIUM,
                1L,
                "Project",
                10L,
                "creator",
                20L,
                "assignee",
                dueDate,
                overdue,
                now,
                now);
    }

    private TaskDetailsResponse taskDetails(Long id, String title) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new TaskDetailsResponse(
                id,
                title,
                "description",
                Status.TODO,
                Priority.MEDIUM,
                1L,
                10L,
                "creator",
                null,
                null,
                null,
                now,
                now,
                List.of(),
                List.of());
    }
}
