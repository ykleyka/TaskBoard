package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);

    void deleteAllByAuthorId(Long authorId);
}
