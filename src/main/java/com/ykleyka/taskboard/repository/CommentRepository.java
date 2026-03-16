package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findAllByTaskId(Long taskId, Pageable pageable);

    void deleteAllByAuthorId(Long authorId);
}
