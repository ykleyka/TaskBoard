package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Override
    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Page<Task> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Optional<Task> findById(Long id);

    @EntityGraph(
            attributePaths = {"project", "creator", "assignee", "tags", "comments", "comments.author"})
    Optional<Task> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    List<Task> findAllByCreatorIdOrAssigneeId(Long creatorId, Long assigneeId);

    @EntityGraph(attributePaths = {"tags", "comments"})
    List<Task> findAllByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Page<Task> findAllByStatus(Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Page<Task> findAllByAssigneeUsernameIgnoreCase(String assignee, Pageable pageable);

    boolean existsByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    Page<Task> findAllByStatusAndAssigneeUsernameIgnoreCase(
            Status status, String assignee, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    @Query("""
            SELECT DISTINCT t
            FROM Task t
            JOIN t.project p
            JOIN p.members member
            LEFT JOIN t.assignee a
            WHERE member.user.id = :userId
              AND (:status IS NULL OR t.status = :status)
              AND (:assignee IS NULL OR lower(a.username) = :assignee)
            """)
    Page<Task> findAllVisibleToUser(
            @Param("userId") Long userId,
            @Param("status") Status status,
            @Param("assignee") String assignee,
            Pageable pageable);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    @Query("""
            SELECT DISTINCT t
            FROM Task t
            JOIN t.project p
            JOIN p.members member
            WHERE member.user.id = :userId
            """)
    List<Task> findAllVisibleToUserList(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"project", "creator", "assignee"})
    @Query("""
            SELECT DISTINCT t
            FROM Task t
            JOIN t.project p
            JOIN t.tags tag
            LEFT JOIN t.assignee a
            WHERE p.id = :projectId
              AND lower(tag.name) = :tagName
              AND (:status IS NULL OR t.status = :status)
              AND (:assigneePattern IS NULL OR lower(a.username) LIKE :assigneePattern)
            """)
    Page<Task> searchByProjectIdAndTag(
            @Param("projectId") Long projectId,
            @Param("tagName") String tagName,
            @Param("status") Status status,
            @Param("assigneePattern") String assigneePattern,
            Pageable pageable);

    @Query(
            value = """
                    SELECT DISTINCT t.*
                    FROM tasks t
                    JOIN task_tags tt ON t.id = tt.task_id
                    JOIN tags tg ON tt.tag_id = tg.id
                    LEFT JOIN users a ON t.assignee_id = a.id
                    WHERE t.project_id = :projectId
                      AND lower(tg.name) = :tagName
                      AND (CAST(:statusValue AS text) IS NULL OR t.status = :statusValue)
                      AND (CAST(:assigneePattern AS text) IS NULL OR lower(a.username) LIKE :assigneePattern)
                      AND t.due_date IS NOT NULL
                      AND t.due_date < :dueBefore
                      AND t.status <> 'COMPLETED'
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT t.id)
                    FROM tasks t
                    JOIN task_tags tt ON t.id = tt.task_id
                    JOIN tags tg ON tt.tag_id = tg.id
                    LEFT JOIN users a ON t.assignee_id = a.id
                    WHERE t.project_id = :projectId
                      AND lower(tg.name) = :tagName
                      AND (CAST(:statusValue AS text) IS NULL OR t.status = :statusValue)
                      AND (CAST(:assigneePattern AS text) IS NULL OR lower(a.username) LIKE :assigneePattern)
                      AND t.due_date IS NOT NULL
                      AND t.due_date < :dueBefore
                      AND t.status <> 'COMPLETED'
                    """,
            nativeQuery = true)
    Page<Task> searchOverdueByProjectIdAndTagNative(
            @Param("projectId") Long projectId,
            @Param("tagName") String tagName,
            @Param("statusValue") String statusValue,
            @Param("assigneePattern") String assigneePattern,
            @Param("dueBefore") Instant dueBefore,
            Pageable pageable);
}
