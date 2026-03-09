package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Project;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(attributePaths = {"members", "members.user", "tasks", "tasks.creator", "tasks.assignee"})
    Optional<Project> findDetailedById(Long id);
}
