package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectMemberRoleRequest;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectTaskSummaryResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.exception.ProjectConflictException;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.ProjectMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    private ProjectMapper mapper;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectCache projectCache;
    @Mock
    private TagCache tagCache;
    @Mock
    private CommentCache commentCache;
    @Mock
    private TaskCache taskCache;

    @InjectMocks
    private ProjectService service;

    @Test
    void getProjects_whenCacheHit_returnsCachedList() {
        Pageable pageable = PageRequest.of(0, 20);
        List<ProjectResponse> cached = List.of(projectResponse(1L, "Cached"));
        when(projectCache.getProjects(any())).thenReturn(cached);

        List<ProjectResponse> actual = service.getProjects(pageable);

        assertEquals(cached, actual);
        verify(projectRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getProjects_whenCacheMiss_loadsFromRepositoryAndCaches() {
        Pageable pageable = PageRequest.of(0, 20);
        Project entity = project(1L, "Repo");
        ProjectResponse mapped = projectResponse(1L, "Repo");

        when(projectCache.getProjects(any())).thenReturn(null);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toResponse(entity)).thenReturn(mapped);

        List<ProjectResponse> actual = service.getProjects(pageable);

        assertEquals(List.of(mapped), actual);
        verify(projectCache).putProjects(any(), any());
    }

    @Test
    void getProjectById_whenCacheHit_returnsCachedDetails() {
        Long id = 10L;
        ProjectDetailsResponse cached = projectDetails(id, "Cached details");
        when(projectCache.getProjectDetails(id)).thenReturn(cached);

        ProjectDetailsResponse actual = service.getProjectById(id);

        assertEquals(cached, actual);
        verify(projectRepository, never()).findDetailedById(anyLong());
    }

    @Test
    void getProjectById_whenCacheMiss_loadsAndCaches() {
        Long id = 11L;
        Project entity = project(id, "Entity");
        ProjectDetailsResponse mapped = projectDetails(id, "Entity");

        when(projectCache.getProjectDetails(id)).thenReturn(null);
        when(projectRepository.findDetailedById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDetailsResponse(entity)).thenReturn(mapped);

        ProjectDetailsResponse actual = service.getProjectById(id);

        assertEquals(mapped, actual);
        verify(projectCache).putProjectDetails(id, mapped);
    }

    @Test
    void getProjectById_whenMissing_throwsProjectNotFound() {
        Long id = 12L;
        when(projectCache.getProjectDetails(id)).thenReturn(null);
        when(projectRepository.findDetailedById(id)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.getProjectById(id));
    }

    @Test
    void getProjectMembers_whenProjectMissing_throwsProjectNotFound() {
        Long id = 13L;
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> service.getProjectMembers(id));
    }

    @Test
    void getProjectMembers_whenProjectExists_returnsSortedMembers() {
        Long projectId = 14L;
        User second = user(22L, "bravo");
        User first = user(11L, "alpha");
        ProjectMember secondMembership = projectMember(projectId, second, ProjectRole.MANAGER);
        ProjectMember firstMembership = projectMember(projectId, first, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findAllByProjectId(projectId))
                .thenReturn(List.of(secondMembership, firstMembership));

        List<ProjectUserSummaryResponse> actual = service.getProjectMembers(projectId);

        assertEquals(2, actual.size());
        assertEquals(11L, actual.get(0).id());
        assertEquals("alpha", actual.get(0).username());
        assertEquals(ProjectRole.MEMBER, actual.get(0).role());
        assertEquals(22L, actual.get(1).id());
        assertEquals(ProjectRole.MANAGER, actual.get(1).role());
    }

    @Test
    void getProjectMember_whenMemberMissing_throwsNotFound() {
        Long projectId = 15L;
        Long userId = 99L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.getProjectMember(projectId, userId));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getProjectMember_whenMemberExists_returnsSummary() {
        Long projectId = 16L;
        Long userId = 33L;
        User user = user(userId, "charlie");
        ProjectMember membership = projectMember(projectId, user, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.of(membership));

        ProjectUserSummaryResponse actual = service.getProjectMember(projectId, userId);

        assertEquals(userId, actual.id());
        assertEquals("charlie", actual.username());
        assertEquals(ProjectRole.MEMBER, actual.role());
    }

    @Test
    void updateProjectMember_whenValid_updatesRoleAndInvalidatesCache() {
        Long projectId = 17L;
        Long userId = 44L;
        User user = user(userId, "delta");
        ProjectMember membership = projectMember(projectId, user, ProjectRole.MEMBER);
        ProjectMemberRoleRequest request = new ProjectMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.of(membership));
        when(projectMemberRepository.save(membership)).thenReturn(membership);

        ProjectUserSummaryResponse actual = service.updateProjectMember(projectId, userId, request);

        assertEquals(ProjectRole.MANAGER, membership.getRole());
        assertEquals(ProjectRole.MANAGER, actual.role());
        verify(projectCache).invalidate();
    }

    @Test
    void updateProjectMember_whenMemberMissing_throwsNotFound() {
        Long projectId = 171L;
        Long userId = 441L;
        ProjectMemberRoleRequest request = new ProjectMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.updateProjectMember(projectId, userId, request));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void deleteProjectMember_whenValid_deletesMemberAndInvalidatesCaches() {
        Long projectId = 18L;
        Long userId = 55L;
        User user = user(userId, "echo");
        ProjectMember membership = projectMember(projectId, user, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.of(membership));

        ProjectUserSummaryResponse actual = service.deleteProjectMember(projectId, userId);

        assertEquals(userId, actual.id());
        verify(projectMemberRepository).delete(membership);
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void deleteProjectMember_whenMemberMissing_throwsNotFound() {
        Long projectId = 181L;
        Long userId = 551L;

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));
        when(projectMemberRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.deleteProjectMember(projectId, userId));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void createProject_whenOwnerIdMissing_throwsBadRequest() {
        ProjectRequest request = new ProjectRequest("Project", "Desc", null);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createProject(request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createProject_whenValid_createsOwnerMembershipAndInvalidatesCache() {
        Long ownerId = 7L;
        ProjectRequest request = new ProjectRequest("Project", "Desc", ownerId);
        User owner = user(ownerId, "owner");
        Project toSave = project(null, "Project");
        Project saved = project(100L, "Project");
        ProjectResponse expected = projectResponse(100L, "Project");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(mapper.toEntity(request)).thenReturn(toSave);
        when(projectRepository.save(toSave)).thenReturn(saved);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(saved)).thenReturn(expected);

        ProjectResponse actual = service.createProject(request);

        assertEquals(expected, actual);
        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(captor.capture());
        ProjectMember savedMembership = captor.getValue();
        assertEquals(ownerId, savedMembership.getUser().getId());
        assertEquals(ProjectRole.OWNER, savedMembership.getRole());
        assertNotNull(savedMembership.getJoinedAt());
        verify(projectCache).invalidate();
    }

    @Test
    void createProject_whenOwnerMissing_throwsUserNotFound() {
        ProjectRequest request = new ProjectRequest("Project", "Desc", 77L);
        when(userRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.createProject(request));
    }

    @Test
    void updateProject_whenValid_updatesAndInvalidatesCaches() {
        Long id = 5L;
        Project existing = project(id, "Old");
        ProjectRequest request = new ProjectRequest("New", "New desc", 1L);
        ProjectResponse mapped = projectResponse(id, "New");

        when(projectRepository.findById(id)).thenReturn(Optional.of(existing));
        when(projectRepository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(mapped);

        ProjectResponse actual = service.updateProject(id, request);

        assertEquals(mapped, actual);
        assertEquals("New", existing.getName());
        assertEquals("New desc", existing.getDescription());
        assertNotNull(existing.getUpdatedAt());
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void updateProject_whenMissing_throwsProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());
        ProjectRequest request = new ProjectRequest("x", "y", 1L);

        assertThrows(ProjectNotFoundException.class, () -> service.updateProject(1L, request));
    }

    @Test
    void patchProject_whenNoChanges_onlyMapsResponse() {
        Long id = 9L;
        Project existing = project(id, "Same");
        ProjectResponse mapped = projectResponse(id, "Same");

        when(projectRepository.findById(id)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(mapped);

        ProjectResponse actual = service.patchProject(id, new ProjectPatchRequest(null, null));

        assertEquals(mapped, actual);
        verify(projectRepository, never()).save(any(Project.class));
        verify(projectCache, never()).invalidate();
        verify(taskCache, never()).invalidate();
    }

    @Test
    void patchProject_whenChanged_savesAndInvalidatesCaches() {
        Long id = 10L;
        Project existing = project(id, "Old");
        ProjectResponse mapped = projectResponse(id, "Updated");

        when(projectRepository.findById(id)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(mapped);

        ProjectResponse actual = service.patchProject(id, new ProjectPatchRequest("Updated", "Desc"));

        assertEquals(mapped, actual);
        verify(projectRepository).save(existing);
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
        assertEquals("Updated", existing.getName());
        assertEquals("Desc", existing.getDescription());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void addMember_whenValidAndRoleNull_usesMemberRole() {
        Long projectId = 200L;
        Long userId = 11L;
        Project project = project(projectId, "Project");
        User user = user(userId, "alpha");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUserSummaryResponse actual =
                service.addMember(projectId, new ProjectMemberRequest(userId, null));

        assertEquals(userId, actual.id());
        assertEquals(ProjectRole.MEMBER, actual.role());
        verify(projectCache).invalidate();
    }

    @Test
    void addMember_whenAlreadyMember_throwsConflict() {
        Long projectId = 300L;
        Long userId = 15L;
        Project project = project(projectId, "Project");
        User user = user(userId, "dup");
        ProjectMemberRequest request = new ProjectMemberRequest(userId, ProjectRole.MANAGER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        assertThrows(ProjectConflictException.class, () -> service.addMember(projectId, request));
    }

    @Test
    void addMember_whenRequestNull_throwsBadRequest() {
        Long projectId = 301L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project(projectId, "Project")));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.addMember(projectId, null));

        assertEquals(400, exception.getStatusCode().value());
        verify(projectCache, never()).invalidate();
    }

    @Test
    void addMembersBulk_whenMiddleUserMissing_savesFirstMemberAndThrows() {
        Long projectId = 400L;
        Long firstUserId = 10L;
        Long missingUserId = 999L;
        Long thirdUserId = 30L;
        Project project = project(projectId, "Project");
        User firstUser = user(firstUserId, "first");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, firstUserId))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ProjectMemberRequest> requests =
                List.of(
                        new ProjectMemberRequest(firstUserId, ProjectRole.MEMBER),
                        new ProjectMemberRequest(missingUserId, ProjectRole.MEMBER),
                        new ProjectMemberRequest(thirdUserId, ProjectRole.MEMBER));

        assertThrows(UserNotFoundException.class, () -> service.addMembersBulk(projectId, requests));

        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(projectId, thirdUserId);
        verify(projectCache).invalidate();
    }

    @Test
    void addMembersBulk_whenValidRequests_addsAllAndUsesDefaultRole() {
        Long projectId = 500L;
        Long firstUserId = 11L;
        Long secondUserId = 22L;
        Project project = project(projectId, "Project");
        User firstUser = user(firstUserId, "alpha");
        User secondUser = user(secondUserId, "bravo");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(firstUserId)).thenReturn(Optional.of(firstUser));
        when(userRepository.findById(secondUserId)).thenReturn(Optional.of(secondUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, firstUserId))
                .thenReturn(false);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, secondUserId))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ProjectMemberRequest> requests =
                List.of(
                        new ProjectMemberRequest(firstUserId, null),
                        new ProjectMemberRequest(secondUserId, ProjectRole.MANAGER));

        List<ProjectUserSummaryResponse> responses = service.addMembersBulk(projectId, requests);

        assertEquals(2, responses.size());
        assertEquals(ProjectRole.MEMBER, responses.get(0).role());
        assertEquals(ProjectRole.MANAGER, responses.get(1).role());
        verify(projectMemberRepository, times(2)).save(any(ProjectMember.class));
        verify(projectCache).invalidate();
    }

    @Test
    void addMembersBulk_whenRequestContainsDuplicateUserIds_throwsConflict() {
        Long projectId = 600L;
        Long duplicatedUserId = 15L;
        Project project = project(projectId, "Project");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        List<ProjectMemberRequest> requests =
                List.of(
                        new ProjectMemberRequest(duplicatedUserId, ProjectRole.MEMBER),
                        new ProjectMemberRequest(duplicatedUserId, ProjectRole.MANAGER));

        assertThrows(ProjectConflictException.class, () -> service.addMembersBulk(projectId, requests));

        verify(userRepository, never()).findById(anyLong());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        verify(projectCache).invalidate();
    }

    @Test
    void addMembersBulk_whenRequestIsNull_throwsBadRequest() {
        Long projectId = 700L;
        Project project = project(projectId, "Project");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.addMembersBulk(projectId, null));

        assertEquals(400, exception.getStatusCode().value());
        verify(projectCache).invalidate();
    }

    @Test
    void addMembersBulk_whenRequestIsEmpty_throwsBadRequest() {
        Long projectId = 710L;
        Project project = project(projectId, "Project");
        List<ProjectMemberRequest> requests = List.of();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.addMembersBulk(projectId, requests));

        assertEquals(400, exception.getStatusCode().value());
        verify(projectCache).invalidate();
    }

    @Test
    void deleteProject_whenTasksExist_deletesTasksProjectMembersAndInvalidatesAllCaches() {
        Long projectId = 800L;
        Project project = project(projectId, "Project");
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);
        ProjectResponse mapped = projectResponse(projectId, "Project");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskRepository.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));
        when(mapper.toResponse(project)).thenReturn(mapped);

        ProjectResponse actual = service.deleteProject(projectId);

        assertEquals(mapped, actual);
        verify(taskRepository).delete(task1);
        verify(taskRepository).delete(task2);
        verify(projectMemberRepository).deleteAllByProjectId(projectId);
        verify(projectRepository).delete(project);
        verify(projectCache).invalidate();
        verify(tagCache).invalidate();
        verify(commentCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void deleteProject_whenNoTasks_stillDeletesProject() {
        Long projectId = 801L;
        Project project = project(projectId, "Project");
        ProjectResponse mapped = projectResponse(projectId, "Project");

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskRepository.findAllByProjectId(projectId)).thenReturn(List.of());
        when(mapper.toResponse(project)).thenReturn(mapped);

        ProjectResponse actual = service.deleteProject(projectId);

        assertEquals(mapped, actual);
        verify(projectMemberRepository).deleteAllByProjectId(projectId);
        verify(projectRepository).delete(project);
    }

    @Test
    void createProject_whenMapperFails_doesNotTouchMembershipRepository() {
        Long ownerId = 900L;
        ProjectRequest request = new ProjectRequest("Project", "Desc", ownerId);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(user(ownerId, "owner")));
        when(mapper.toEntity(request)).thenThrow(new RuntimeException("mapper-failed"));

        assertThrows(RuntimeException.class, () -> service.createProject(request));

        verifyNoInteractions(projectMemberRepository);
    }

    private Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("description");
        project.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        project.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
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
        Project project = new Project();
        project.setId(projectId);
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private ProjectResponse projectResponse(Long id, String name) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ProjectResponse(id, name, "description", now, now);
    }

    private ProjectDetailsResponse projectDetails(Long id, String name) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ProjectDetailsResponse(
                id,
                name,
                "description",
                now,
                now,
                1,
                1,
                0,
                List.of(new ProjectUserSummaryResponse(1L, "user", ProjectRole.MEMBER)),
                List.of(
                        new ProjectTaskSummaryResponse(
                                1L,
                                "Task",
                                Status.TODO,
                                Priority.MEDIUM,
                                1L,
                                "creator",
                                null,
                                null,
                                null,
                                now,
                                now,
                                List.of())));
    }
}
