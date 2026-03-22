package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.CommentRequest;
import com.ykleyka.taskboard.dto.CommentResponse;
import com.ykleyka.taskboard.service.CommentService;
import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Operations for managing task comments")
public class CommentController {
    private final CommentService service;

    @Operation(summary = "List task comments", description = "Returns comments for the specified task.")
    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> getCommentsByTaskId(
            @PathVariable @Positive Long taskId,
            @ParameterObject
                    @PageableDefault(page = 0, size = 20, sort = "createdAt")
                    Pageable pageable) {
        return service.getCommentsByTaskId(taskId, pageable);
    }

    @Operation(summary = "Create comment", description = "Creates a new comment for the specified task.")
    @PostMapping("/tasks/{taskId}/comments")
    public CommentResponse createComment(
            @PathVariable @Positive Long taskId,
            @Validated({Default.class, OnCreate.class}) @RequestBody CommentRequest request) {
        return service.createComment(taskId, request);
    }

    @Operation(summary = "Replace comment", description = "Fully updates an existing comment.")
    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CommentRequest request) {
        return service.updateComment(id, request);
    }

    @Operation(summary = "Delete comment", description = "Deletes a comment and returns the removed entity.")
    @DeleteMapping("/comments/{id}")
    public CommentResponse deleteComment(@PathVariable @Positive Long id) {
        return service.deleteComment(id);
    }
}
