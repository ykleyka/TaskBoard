package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Project;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(
            attributePaths = {
                "members",
                "members.user",
                "tasks",
                "tasks.creator",
                "tasks.assignee",
                "tasks.tags"
            })
    Optional<Project> findDetailedById(Long id);

    @Query("""
            SELECT DISTINCT p
            FROM Project p
            JOIN p.members member
            WHERE member.user.id = :userId
            """)
    Page<Project> findAllVisibleToUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT p.id)
            FROM Project p
            JOIN p.members member
            WHERE member.user.id = :userId
            """)
    long countVisibleToUser(@Param("userId") Long userId);
}
