package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Override
    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Optional<Task> findById(Long id);

    @EntityGraph(
            attributePaths = {"project", "creator", "assignee", "tags", "comments", "comments.author"})
    Optional<Task> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAllByCreatorIdOrAssigneeId(Long creatorId, Long assigneeId);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAllByStatus(Status status, Sort sort);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAllByAssigneeUsernameIgnoreCase(String assignee, Sort sort);

    boolean existsByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAllByStatusAndAssigneeUsernameIgnoreCase(
            Status status, String assignee, Sort sort);
}
