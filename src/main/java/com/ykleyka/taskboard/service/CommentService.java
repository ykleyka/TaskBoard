package com.ykleyka.taskboard.service;

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
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper mapper;
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public List<CommentResponse> getCommentsByTaskId(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
        return commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CommentResponse createComment(Long taskId, CommentRequest request) {
        if (request.authorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authorId is required");
        }
        Comment comment = mapper.toEntity(request);
        comment.setTask(findTask(taskId));
        comment.setAuthor(findUser(request.authorId()));
        return mapper.toResponse(commentRepository.save(comment));
    }

    public CommentResponse updateComment(Long id, CommentRequest request) {
        Comment comment = findComment(id);
        comment.setText(request.text());
        comment.setUpdatedAt(Instant.now());
        return mapper.toResponse(commentRepository.save(comment));
    }

    public CommentResponse deleteComment(Long id) {
        Comment comment = findComment(id);
        commentRepository.delete(comment);
        return mapper.toResponse(comment);
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
}
