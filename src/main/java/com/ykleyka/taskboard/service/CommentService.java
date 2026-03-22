package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.CommentPageKey;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.CommentRequest;
import com.ykleyka.taskboard.dto.CommentResponse;
import com.ykleyka.taskboard.exception.CommentNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.CommentMapper;
import com.ykleyka.taskboard.model.Comment;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.repository.CommentRepository;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper mapper;
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CommentCache commentCache;
    private final TaskCache taskCache;

    public List<CommentResponse> getCommentsByTaskId(Long taskId, Pageable pageable) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
        CommentPageKey key = CommentPageKey.from(taskId, pageable);
        List<CommentResponse> cached = commentCache.getByTaskId(key);
        if (cached != null) {
            log.info(
                    "Comments returned from cache: taskId={}, page={}, size={}, sort={}",
                    key.getTaskId(),
                    key.getPage(),
                    key.getSize(),
                    key.getSort());
            return cached;
        }
        List<CommentResponse> content =
                commentRepository.findAllByTaskId(taskId, pageable).map(mapper::toResponse)
                        .getContent();
        commentCache.putByTaskId(key, content);
        return content;
    }

    public CommentResponse createComment(Long taskId, CommentRequest request) {
        if (request.authorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorId is required");
        }
        Comment comment = mapper.toEntity(request);
        Task task = findTask(taskId);
        User author = findUser(request.authorId());
        ensureProjectMember(task, author);
        comment.setTask(task);
        comment.setAuthor(author);
        CommentResponse response = mapper.toResponse(commentRepository.save(comment));
        commentCache.invalidateTask(taskId);
        taskCache.invalidateTaskDetails(taskId);
        return response;
    }

    public CommentResponse updateComment(Long id, CommentRequest request) {
        Comment comment = findComment(id);
        ensureProjectMember(comment.getTask(), comment.getAuthor());
        comment.setText(request.text());
        comment.setUpdatedAt(Instant.now());
        CommentResponse response = mapper.toResponse(commentRepository.save(comment));
        if (comment.getTask() != null) {
            commentCache.invalidateTask(comment.getTask().getId());
            taskCache.invalidateTaskDetails(comment.getTask().getId());
        }
        return response;
    }

    public CommentResponse deleteComment(Long id) {
        Comment comment = findComment(id);
        ensureProjectMember(comment.getTask(), comment.getAuthor());
        commentRepository.delete(comment);
        CommentResponse response = mapper.toResponse(comment);
        if (comment.getTask() != null) {
            commentCache.invalidateTask(comment.getTask().getId());
            taskCache.invalidateTaskDetails(comment.getTask().getId());
        }
        return response;
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void ensureProjectMember(Task task, User user) {
        if (task == null || task.getProject() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task has no project");
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required");
        }
        Long projectId = task.getProject().getId();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "author must be a member of project " + projectId);
        }
    }
}
