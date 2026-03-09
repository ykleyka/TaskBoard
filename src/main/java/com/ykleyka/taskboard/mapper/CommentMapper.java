package com.ykleyka.taskboard.mapper;

import com.ykleyka.taskboard.dto.CommentRequest;
import com.ykleyka.taskboard.dto.CommentResponse;
import com.ykleyka.taskboard.model.Comment;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest request) {
        Comment comment = new Comment();
        comment.setText(request.text());
        Instant now = Instant.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        return comment;
    }

    public CommentResponse toResponse(Comment comment) {
        String fullName = null;
        if (comment.getAuthor() != null) {
            String first = comment.getAuthor().getFirstName() == null ? "" : comment.getAuthor().getFirstName();
            String last = comment.getAuthor().getLastName() == null ? "" : comment.getAuthor().getLastName();
            String joined = (first + " " + last).trim();
            fullName = joined.isEmpty() ? null : joined;
        }

        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getTask() == null ? null : comment.getTask().getId(),
                comment.getAuthor() == null ? null : comment.getAuthor().getId(),
                comment.getAuthor() == null ? null : comment.getAuthor().getUsername(),
                fullName,
                comment.getUpdatedAt() != null && !comment.getUpdatedAt().equals(comment.getCreatedAt()),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
