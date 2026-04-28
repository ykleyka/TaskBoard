package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.CommentRequest;
import com.ykleyka.taskboard.dto.CommentResponse;
import com.ykleyka.taskboard.exception.CommentNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.CommentMapper;
import com.ykleyka.taskboard.model.Comment;
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
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    private CommentMapper mapper;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private CommentCache commentCache;
    @Mock
    private TaskCache taskCache;

    @InjectMocks
    private CommentService service;

    @Test
    void getCommentsByTaskId_whenTaskMissing_throwsTaskNotFound() {
        Long taskId = 42L;
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.existsById(taskId)).thenReturn(false);

        assertThrows(TaskNotFoundException.class, () -> service.getCommentsByTaskId(taskId, pageable));

        verifyNoInteractions(commentRepository);
    }

    @Test
    void getCommentsByTaskId_whenCacheHit_returnsCachedValue() {
        Long taskId = 43L;
        Pageable pageable = PageRequest.of(0, 20);
        List<CommentResponse> cached = List.of(commentResponse(taskId, 1L));

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(commentCache.getByTaskId(any())).thenReturn(cached);

        List<CommentResponse> actual = service.getCommentsByTaskId(taskId, pageable);

        assertEquals(cached, actual);
        verify(commentRepository, never()).findAllByTaskId(any(), any(Pageable.class));
    }

    @Test
    void getCommentsByTaskId_whenCacheMiss_loadsAndCaches() {
        Long taskId = 44L;
        Pageable pageable = PageRequest.of(0, 20);
        Comment comment = comment(1L, task(taskId, 3L), user(7L, "author"));
        CommentResponse mapped = commentResponse(taskId, 7L);

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(commentCache.getByTaskId(any())).thenReturn(null);
        when(commentRepository.findAllByTaskId(taskId, pageable)).thenReturn(new PageImpl<>(List.of(comment)));
        when(mapper.toResponse(comment)).thenReturn(mapped);

        List<CommentResponse> actual = service.getCommentsByTaskId(taskId, pageable);

        assertEquals(List.of(mapped), actual);
        verify(commentCache).putByTaskId(any(), any());
    }

    @Test
    void getCommentsByTaskId_authenticatedWhenUserIsMember_returnsComments() {
        Long taskId = 45L;
        Long projectId = 4L;
        Long currentUserId = 20L;
        Pageable pageable = PageRequest.of(0, 20);
        List<CommentResponse> cached = List.of(commentResponse(taskId, currentUserId));

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(taskId, projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(commentCache.getByTaskId(any())).thenReturn(cached);

        List<CommentResponse> actual =
                service.getCommentsByTaskId(taskId, pageable, currentUserId);

        assertEquals(cached, actual);
    }

    @Test
    void getCommentsByTaskId_authenticatedWhenUserIsNotMember_throwsNotFound() {
        Long taskId = 46L;
        Long projectId = 5L;
        Long currentUserId = 21L;
        Pageable pageable = PageRequest.of(0, 20);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(taskId, projectId)));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.getCommentsByTaskId(taskId, pageable, currentUserId));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void createComment_whenAuthorIdMissing_throwsBadRequest() {
        CommentRequest request = new CommentRequest(null, "text");

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createComment(1L, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createComment_whenTaskMissing_throwsTaskNotFound() {
        Long taskId = 2L;
        CommentRequest request = new CommentRequest(1L, "text");
        when(mapper.toEntity(request)).thenReturn(new Comment());
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.createComment(taskId, request));
    }

    @Test
    void createComment_whenAuthorMissing_throwsUserNotFound() {
        Long taskId = 3L;
        CommentRequest request = new CommentRequest(9L, "text");
        when(mapper.toEntity(request)).thenReturn(new Comment());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task(taskId, 5L)));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.createComment(taskId, request));
    }

    @Test
    void createComment_whenTaskHasNoProject_throwsConflict() {
        Long taskId = 4L;
        Long authorId = 10L;
        Task task = new Task();
        task.setId(taskId);
        CommentRequest request = new CommentRequest(authorId, "text");

        when(mapper.toEntity(request)).thenReturn(new Comment());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(user(authorId, "author")));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createComment(taskId, request));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void createComment_whenAuthorNotProjectMember_throwsBadRequest() {
        Long taskId = 5L;
        Long projectId = 3L;
        Long authorId = 7L;
        Task task = task(taskId, projectId);
        User author = user(authorId, "author");
        CommentRequest request = new CommentRequest(authorId, "Looks good");

        when(mapper.toEntity(request)).thenReturn(new Comment());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.createComment(taskId, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void createComment_whenValidRequest_setsRelationsAndInvalidatesCaches() {
        Long taskId = 6L;
        Long projectId = 8L;
        Long authorId = 9L;
        Task task = task(taskId, projectId);
        User author = user(authorId, "author");
        CommentRequest request = new CommentRequest(authorId, "Looks good");
        Comment mapped = new Comment();
        CommentResponse expected = commentResponse(taskId, authorId);

        when(mapper.toEntity(request)).thenReturn(mapped);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Comment.class))).thenReturn(expected);

        CommentResponse actual = service.createComment(taskId, request);

        assertEquals(expected, actual);
        assertEquals(task, mapped.getTask());
        assertEquals(author, mapped.getAuthor());
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    @Test
    void createComment_authenticatedUsesCurrentUserAsAuthor() {
        Long taskId = 61L;
        Long projectId = 81L;
        Long currentUserId = 91L;
        Task task = task(taskId, projectId);
        User author = user(currentUserId, "current");
        CommentRequest request = new CommentRequest(null, "Authenticated comment");
        Comment mapped = new Comment();
        CommentResponse expected = commentResponse(taskId, currentUserId);

        when(mapper.toEntity(request)).thenReturn(mapped);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(author));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(commentRepository.save(mapped)).thenReturn(mapped);
        when(mapper.toResponse(mapped)).thenReturn(expected);

        CommentResponse actual = service.createComment(taskId, request, currentUserId);

        assertEquals(expected, actual);
        assertEquals(task, mapped.getTask());
        assertEquals(author, mapped.getAuthor());
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    @Test
    void updateComment_whenMissing_throwsCommentNotFound() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());
        CommentRequest request = new CommentRequest(1L, "updated");

        assertThrows(CommentNotFoundException.class, () -> service.updateComment(100L, request));
    }

    @Test
    void updateComment_whenTaskMissingInComment_throwsConflict() {
        Comment comment = comment(1001L, null, user(4L, "author"));
        CommentRequest request = new CommentRequest(4L, "updated");
        when(commentRepository.findById(1001L)).thenReturn(Optional.of(comment));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.updateComment(1001L, request));

        assertEquals(409, exception.getStatusCode().value());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateComment_whenAuthorMissingInComment_throwsBadRequest() {
        Comment comment = comment(101L, task(2L, 3L), null);
        CommentRequest request = new CommentRequest(1L, "updated");
        when(commentRepository.findById(101L)).thenReturn(Optional.of(comment));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.updateComment(101L, request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void updateComment_whenValidAndTaskPresent_invalidatesTaskCaches() {
        Long taskId = 7L;
        Long projectId = 11L;
        Long authorId = 12L;
        Comment comment = comment(102L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(102L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(true);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual = service.updateComment(102L, new CommentRequest(authorId, "updated"));

        assertEquals(expected, actual);
        assertEquals("updated", comment.getText());
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    @Test
    void updateComment_whenTaskBecomesNullAfterSave_skipsInvalidationBranch() {
        Long taskId = 8L;
        Long projectId = 12L;
        Long authorId = 13L;
        Comment comment = comment(103L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(103L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(true);
        when(commentRepository.save(comment))
                .thenAnswer(
                        invocation -> {
                            comment.setTask(null);
                            return comment;
                        });
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual = service.updateComment(103L, new CommentRequest(authorId, "updated"));

        assertEquals(expected, actual);
        verify(commentCache, never()).invalidateTask(any());
        verify(taskCache, never()).invalidateTaskDetails(any());
    }

    @Test
    void updateComment_authenticatedWhenCurrentUserIsAuthor_updatesComment() {
        Long taskId = 71L;
        Long projectId = 111L;
        Long currentUserId = 121L;
        Comment comment = comment(301L, task(taskId, projectId), user(currentUserId, "author"));
        CommentResponse expected = commentResponse(taskId, currentUserId);

        when(commentRepository.findById(301L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
                .thenReturn(true);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual =
                service.updateComment(301L, new CommentRequest(null, "author update"), currentUserId);

        assertEquals(expected, actual);
        assertEquals("author update", comment.getText());
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    @Test
    void updateComment_authenticatedWhenCurrentUserIsManager_updatesComment() {
        Long taskId = 72L;
        Long projectId = 112L;
        Long authorId = 122L;
        Long managerId = 123L;
        Comment comment = comment(302L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(302L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, managerId))
                .thenReturn(true);
        when(projectMemberRepository.findById(new ProjectMemberId(projectId, managerId)))
                .thenReturn(Optional.of(projectMember(projectId, managerId, ProjectRole.MANAGER)));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual =
                service.updateComment(302L, new CommentRequest(null, "manager update"), managerId);

        assertEquals(expected, actual);
        assertEquals("manager update", comment.getText());
    }

    @Test
    void updateComment_authenticatedWhenCurrentUserIsRegularMember_throwsForbidden() {
        Long taskId = 73L;
        Long projectId = 113L;
        Long authorId = 124L;
        Long memberId = 125L;
        Comment comment = comment(303L, task(taskId, projectId), user(authorId, "author"));

        when(commentRepository.findById(303L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, memberId))
                .thenReturn(true);
        when(projectMemberRepository.findById(new ProjectMemberId(projectId, memberId)))
                .thenReturn(Optional.of(projectMember(projectId, memberId, ProjectRole.MEMBER)));
        CommentRequest request = new CommentRequest(null, "member update");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.updateComment(303L, request, memberId));

        assertEquals(403, exception.getStatusCode().value());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteComment_whenMissing_throwsCommentNotFound() {
        when(commentRepository.findById(200L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> service.deleteComment(200L));
    }

    @Test
    void deleteComment_whenAuthorNotProjectMember_throwsBadRequest() {
        Long taskId = 10L;
        Long projectId = 15L;
        Long authorId = 16L;
        Comment comment = comment(201L, task(taskId, projectId), user(authorId, "author"));

        when(commentRepository.findById(201L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.deleteComment(201L));

        assertEquals(400, exception.getStatusCode().value());
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void deleteComment_whenTaskPresent_invalidatesCaches() {
        Long taskId = 11L;
        Long projectId = 17L;
        Long authorId = 18L;
        Comment comment = comment(202L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(202L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(true);
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual = service.deleteComment(202L);

        assertEquals(expected, actual);
        verify(commentRepository).delete(comment);
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    @Test
    void deleteComment_whenTaskBecomesNullAfterDelete_skipsInvalidationBranch() {
        Long taskId = 12L;
        Long projectId = 18L;
        Long authorId = 19L;
        Comment comment = comment(203L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(203L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, authorId)).thenReturn(true);
        doAnswer(
                        invocation -> {
                            comment.setTask(null);
                            return null;
                        })
                .when(commentRepository)
                .delete(comment);
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual = service.deleteComment(203L);

        assertEquals(expected, actual);
        verify(commentCache, never()).invalidateTask(any());
        verify(taskCache, never()).invalidateTaskDetails(any());
    }

    @Test
    void deleteComment_authenticatedWhenCurrentUserIsOwner_deletesComment() {
        Long taskId = 13L;
        Long projectId = 19L;
        Long authorId = 20L;
        Long ownerId = 21L;
        Comment comment = comment(204L, task(taskId, projectId), user(authorId, "author"));
        CommentResponse expected = commentResponse(taskId, authorId);

        when(commentRepository.findById(204L)).thenReturn(Optional.of(comment));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, ownerId))
                .thenReturn(true);
        when(projectMemberRepository.findById(new ProjectMemberId(projectId, ownerId)))
                .thenReturn(Optional.of(projectMember(projectId, ownerId, ProjectRole.OWNER)));
        when(mapper.toResponse(comment)).thenReturn(expected);

        CommentResponse actual = service.deleteComment(204L, ownerId);

        assertEquals(expected, actual);
        verify(commentRepository).delete(comment);
        verify(commentCache).invalidateTask(taskId);
        verify(taskCache).invalidateTaskDetails(taskId);
    }

    private Task task(Long taskId, Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        Task task = new Task();
        task.setId(taskId);
        task.setProject(project);
        return task;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName("Author");
        user.setLastName("User");
        return user;
    }

    private Comment comment(Long id, Task task, User author) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setText("initial");
        comment.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        comment.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return comment;
    }

    private CommentResponse commentResponse(Long taskId, Long authorId) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new CommentResponse(
                1L,
                "text",
                taskId,
                authorId,
                "author",
                "Author User",
                false,
                now,
                now);
    }

    private ProjectMember projectMember(Long projectId, Long userId, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(new ProjectMemberId(projectId, userId));
        member.setRole(role);
        return member;
    }
}
