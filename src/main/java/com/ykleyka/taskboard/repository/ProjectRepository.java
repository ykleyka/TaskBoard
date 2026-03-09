package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
