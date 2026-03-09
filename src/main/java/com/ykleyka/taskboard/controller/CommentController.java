package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.CommentPutRequest;
import com.ykleyka.taskboard.dto.CommentRequest;
import com.ykleyka.taskboard.dto.CommentResponse;
import com.ykleyka.taskboard.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class CommentController {
    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> getCommentsByTaskId(@PathVariable Long taskId) {
        return service.getCommentsByTaskId(taskId);
    }

    @PostMapping("/tasks/{taskId}/comments")
    public CommentResponse createComment(
            @PathVariable Long taskId, @Valid @RequestBody CommentRequest request) {
        return service.createComment(taskId, request);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(
            @PathVariable Long id, @Valid @RequestBody CommentPutRequest request) {
        return service.updateComment(id, request);
    }

    @DeleteMapping("/comments/{id}")
    public CommentResponse deleteComment(@PathVariable Long id) {
        return service.deleteComment(id);
    }
}
