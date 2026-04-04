package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.UserPatchRequest;
import com.ykleyka.taskboard.exception.UserConflictException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.repository.CommentRepository;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectCache projectCache;
    @Mock
    private CommentCache commentCache;
    @Mock
    private TaskCache taskCache;

    @InjectMocks
    private UserService service;

    @Test
    void getUsers_returnsPageContent() {
        User first = user(1L, "first", "f@example.com");
        User second = user(2L, "second", "s@example.com");
        when(userRepository.findAll(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(first, second)));

        List<User> actual = service.getUsers(PageRequest.of(0, 20));

        assertEquals(2, actual.size());
        assertEquals("first", actual.get(0).getUsername());
        assertEquals("second", actual.get(1).getUsername());
    }

    @Test
    void getUserById_whenFound_returnsUser() {
        User user = user(5L, "john", "john@example.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        User actual = service.getUserById(5L);

        assertEquals(user, actual);
    }

    @Test
    void getUserById_whenMissing_throwsUserNotFound() {
        when(userRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUserById(6L));
    }

    @Test
    void createUser_whenUsernameExists_throwsConflict() {
        User user = user(1L, "john", "john@example.com");
        user.setPasswordHash("pwd");
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(true);

        assertThrows(UserConflictException.class, () -> service.createUser(user));
    }

    @Test
    void createUser_whenEmailExists_throwsConflict() {
        User user = user(1L, "john", "john@example.com");
        user.setPasswordHash("pwd");
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThrows(UserConflictException.class, () -> service.createUser(user));
    }

    @Test
    void createUser_whenPasswordBlank_throwsBadRequest() {
        User user = user(1L, "john", "john@example.com");
        user.setPasswordHash("   ");
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createUser(user));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createUser_whenPasswordNull_throwsBadRequest() {
        User user = user(1L, "john", "john@example.com");
        user.setPasswordHash(null);
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createUser(user));

        assertEquals(400, exception.getStatusCode().value());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createUser_whenValidRequest_encodesPasswordAndSetsTimestamps() {
        User user = user(1L, "john", "john@example.com");
        user.setPasswordHash("raw-password");

        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.createUser(user);

        assertEquals("encoded-password", saved.getPasswordHash());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_whenMissing_throwsUserNotFound() {
        when(userRepository.findById(40L)).thenReturn(Optional.empty());
        User request = user(0L, "n", "e");

        assertThrows(UserNotFoundException.class, () -> service.updateUser(40L, request));
    }

    @Test
    void updateUser_whenPasswordProvided_encodesAndInvalidatesCaches() {
        Long userId = 41L;
        User existing = user(userId, "current", "current@example.com");
        existing.setPasswordHash("old");
        User request = user(null, "updated", "updated@example.com");
        request.setPasswordHash("new-password");
        request.setFirstName("Updated");
        request.setLastName("User");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsernameIgnoreCase("updated")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.updateUser(userId, request);

        assertEquals("updated", actual.getUsername());
        assertEquals("updated@example.com", actual.getEmail());
        assertEquals("encoded-new", actual.getPasswordHash());
        assertEquals("Updated", actual.getFirstName());
        assertEquals("User", actual.getLastName());
        verify(commentCache).invalidate();
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void updateUser_whenPasswordIsNull_doesNotEncode() {
        Long userId = 42L;
        User existing = user(userId, "current", "current@example.com");
        existing.setPasswordHash("old-hash");
        User request = user(null, "updated", "updated@example.com");
        request.setPasswordHash(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsernameIgnoreCase("updated")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.updateUser(userId, request);

        assertEquals("old-hash", actual.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_whenUsernameAndEmailBelongToSameUser_doesNotConflict() {
        Long userId = 43L;
        User existing = user(userId, "current", "current@example.com");
        User request = user(null, "current", "current@example.com");
        request.setFirstName("Updated");
        request.setLastName("Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsernameIgnoreCase("current")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("current@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.updateUser(userId, request);

        assertEquals(existing, actual);
        assertEquals("Updated", actual.getFirstName());
        assertEquals("Name", actual.getLastName());
        verify(userRepository).save(existing);
    }

    @Test
    void patchUser_whenUsernameConflict_throwsUserConflict() {
        Long userId = 50L;
        User existing = user(userId, "current", "current@example.com");
        User conflict = user(2L, "taken", "taken@example.com");
        UserPatchRequest request = new UserPatchRequest("taken", null, null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsernameIgnoreCase("taken")).thenReturn(Optional.of(conflict));

        assertThrows(UserConflictException.class, () -> service.patchUser(userId, request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void patchUser_whenEmailConflict_throwsUserConflict() {
        Long userId = 51L;
        User existing = user(userId, "current", "current@example.com");
        User conflict = user(3L, "other", "taken@example.com");
        UserPatchRequest request = new UserPatchRequest(null, "taken@example.com", null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(conflict));

        assertThrows(UserConflictException.class, () -> service.patchUser(userId, request));
    }

    @Test
    void patchUser_whenAllFieldsProvided_updatesEverythingAndInvalidatesCaches() {
        Long userId = 52L;
        User existing = user(userId, "old", "old@example.com");
        UserPatchRequest request =
                new UserPatchRequest(
                        "new-user",
                        "new@example.com",
                        "new-password",
                        "New",
                        "Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsernameIgnoreCase("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("old@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.patchUser(userId, request);

        assertEquals("new-user", actual.getUsername());
        assertEquals("new@example.com", actual.getEmail());
        assertEquals("encoded", actual.getPasswordHash());
        assertEquals("New", actual.getFirstName());
        assertEquals("Name", actual.getLastName());
        assertNotNull(actual.getUpdatedAt());
        verify(commentCache).invalidate();
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void patchUser_whenUsernameAndEmailAreSame_avoidsDuplicateChecks() {
        Long userId = 53L;
        User existing = user(userId, "same", "same@example.com");
        UserPatchRequest request = new UserPatchRequest("same", "same@example.com", null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.patchUser(userId, request);

        assertEquals("same", actual.getUsername());
        assertEquals("same@example.com", actual.getEmail());
        verify(userRepository, never()).findByUsernameIgnoreCase(any());
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void patchUser_whenEmailIsNull_skipsEmailBranchAndKeepsOldEmail() {
        Long userId = 54L;
        User existing = user(userId, "same", "same@example.com");
        UserPatchRequest request = new UserPatchRequest(null, null, null, "OnlyName", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User actual = service.patchUser(userId, request);

        assertEquals("same@example.com", actual.getEmail());
        assertEquals("OnlyName", actual.getFirstName());
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void deleteUser_whenNoAffectedTasks_deletesUserAndRelationsWithoutSaveAll() {
        Long deletedUserId = 60L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId)).thenReturn(List.of());
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());

        User actual = service.deleteUser(deletedUserId);

        assertEquals(deletedUser, actual);
        verify(taskRepository, never()).saveAll(any());
        verify(commentRepository).deleteAllByAuthorId(deletedUserId);
        verify(projectMemberRepository).deleteAllByUserId(deletedUserId);
        verify(userRepository).delete(deletedUser);
        verify(commentCache).invalidate();
        verify(projectCache).invalidate();
        verify(taskCache).invalidate();
    }

    @Test
    void deleteUser_whenTaskHasNoProject_throwsConflict() {
        Long deletedUserId = 61L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");
        Task task = new Task();
        task.setId(300L);
        task.setCreator(deletedUser);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId))
                .thenReturn(List.of(task));
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.deleteUser(deletedUserId));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void deleteUser_whenProjectHasNoReplacementOwner_throwsConflict() {
        Long deletedUserId = 62L;
        Long projectId = 15L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");
        Task task = creatorTask(301L, projectId, deletedUser);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId))
                .thenReturn(List.of(task));
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());
        when(projectMemberRepository.findAllByProjectIdInAndRoleAndUserIdNot(
                anyCollection(), eq(ProjectRole.OWNER), eq(deletedUserId)))
                .thenReturn(List.of());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.deleteUser(deletedUserId));

        assertEquals(409, exception.getStatusCode().value());
        verify(taskRepository, never()).saveAll(any());
        verify(commentRepository, never()).deleteAllByAuthorId(deletedUserId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_whenReplacementOwnerExists_reassignsTasksAndSaves() {
        Long deletedUserId = 63L;
        Long projectId = 20L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");
        User replacementOwner = user(99L, "replacement", "repl@example.com");
        User assignee = user(deletedUserId, "owner", "owner@example.com");

        Task creatorTask = creatorTask(302L, projectId, deletedUser);
        creatorTask.setAssignee(assignee);
        Task unaffected = creatorTask(303L, projectId, replacementOwner);
        unaffected.setAssignee(replacementOwner);

        ProjectMember replacementMembership =
                membership(projectId, replacementOwner, ProjectRole.OWNER);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId))
                .thenReturn(List.of(creatorTask, unaffected));
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());
        when(projectMemberRepository.findAllByProjectIdInAndRoleAndUserIdNot(
                anyCollection(), eq(ProjectRole.OWNER), eq(deletedUserId)))
                .thenReturn(List.of(replacementMembership));

        User actual = service.deleteUser(deletedUserId);

        assertEquals(deletedUser, actual);
        assertEquals(replacementOwner, creatorTask.getCreator());
        assertEquals(replacementOwner, creatorTask.getAssignee());
        assertNotNull(creatorTask.getUpdatedAt());
        verify(taskRepository).saveAll(any());
        verify(commentRepository).deleteAllByAuthorId(deletedUserId);
        verify(projectMemberRepository).deleteAllByUserId(deletedUserId);
        verify(userRepository).delete(deletedUser);
    }

    @Test
    void deleteUser_whenTaskCreatorNullAndAssigneeIsDeletedUser_reassignsAssigneeToNull() {
        Long deletedUserId = 64L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");

        Project project = new Project();
        project.setId(21L);
        Task task = new Task();
        task.setId(304L);
        task.setProject(project);
        task.setCreator(null);
        task.setAssignee(deletedUser);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId))
                .thenReturn(List.of(task));
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());

        User actual = service.deleteUser(deletedUserId);

        assertEquals(deletedUser, actual);
        assertNull(task.getAssignee());
        assertNotNull(task.getUpdatedAt());
        verify(taskRepository).saveAll(any());
    }

    @Test
    void deleteUser_whenDeletedCreatorTaskHasNullAssignee_reassignsCreatorAndKeepsAssigneeNull() {
        Long deletedUserId = 66L;
        Long projectId = 23L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");
        User replacementOwner = user(101L, "replacement", "repl@example.com");
        Task creatorTask = creatorTask(305L, projectId, deletedUser);
        creatorTask.setAssignee(null);
        ProjectMember replacementMembership =
                membership(projectId, replacementOwner, ProjectRole.OWNER);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId))
                .thenReturn(List.of(creatorTask));
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of());
        when(projectMemberRepository.findAllByProjectIdInAndRoleAndUserIdNot(
                anyCollection(), eq(ProjectRole.OWNER), eq(deletedUserId)))
                .thenReturn(List.of(replacementMembership));

        User actual = service.deleteUser(deletedUserId);

        assertEquals(deletedUser, actual);
        assertEquals(replacementOwner, creatorTask.getCreator());
        assertNull(creatorTask.getAssignee());
        assertNotNull(creatorTask.getUpdatedAt());
        verify(taskRepository).saveAll(any());
    }

    @Test
    void deleteUser_whenUserOwnsProjectWithoutTasks_andReplacementOwnerExists_deletesSuccessfully() {
        Long deletedUserId = 65L;
        Long projectId = 22L;
        User deletedUser = user(deletedUserId, "owner", "owner@example.com");
        User replacementOwner = user(100L, "replacement", "repl@example.com");
        ProjectMember ownedProjectMembership = membership(projectId, deletedUser, ProjectRole.OWNER);
        ProjectMember replacementMembership = membership(projectId, replacementOwner, ProjectRole.OWNER);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.of(deletedUser));
        when(taskRepository.findAllByCreatorIdOrAssigneeId(deletedUserId, deletedUserId)).thenReturn(List.of());
        when(projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER))
                .thenReturn(List.of(ownedProjectMembership));
        when(projectMemberRepository.findAllByProjectIdInAndRoleAndUserIdNot(
                anyCollection(), eq(ProjectRole.OWNER), eq(deletedUserId)))
                .thenReturn(List.of(replacementMembership));

        User actual = service.deleteUser(deletedUserId);

        assertEquals(deletedUser, actual);
        verify(taskRepository, never()).saveAll(any());
        verify(projectMemberRepository).deleteAllByUserId(deletedUserId);
        verify(userRepository).delete(deletedUser);
    }

    private User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    private Task creatorTask(Long taskId, Long projectId, User creator) {
        Project project = new Project();
        project.setId(projectId);
        Task task = new Task();
        task.setId(taskId);
        task.setProject(project);
        task.setCreator(creator);
        return task;
    }

    private ProjectMember membership(Long projectId, User user, ProjectRole role) {
        Project project = new Project();
        project.setId(projectId);
        ProjectMember member = new ProjectMember();
        member.setId(new ProjectMemberId(projectId, user.getId()));
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}
