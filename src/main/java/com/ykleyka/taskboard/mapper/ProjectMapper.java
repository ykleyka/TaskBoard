package com.ykleyka.taskboard.mapper;

import com.ykleyka.taskboard.dto.ProjectCreateRequest;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectTaskSummaryResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        Instant now = Instant.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return project;
    }

    public Project toEntity(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        Instant now = Instant.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return project;
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public ProjectDetailsResponse toDetailsResponse(Project project) {
        List<ProjectUserSummaryResponse> users =
                project.getMembers().stream()
                        .map(
                                member ->
                                        new ProjectUserSummaryResponse(
                                                member.getUser().getId(),
                                                member.getUser().getUsername(),
                                                member.getRole()))
                        .sorted(Comparator.comparing(ProjectUserSummaryResponse::id))
                        .toList();

        List<ProjectTaskSummaryResponse> tasks =
                project.getTasks().stream()
                        .map(
                                task ->
                                        new ProjectTaskSummaryResponse(
                                                task.getId(),
                                                task.getTitle(),
                                                task.getStatus(),
                                                task.getPriority(),
                                                task.getCreator() == null
                                                        ? null
                                                        : task.getCreator().getId(),
                                                task.getCreator() == null
                                                        ? null
                                                        : task.getCreator().getUsername(),
                                                task.getAssignee() == null
                                                        ? null
                                                        : task.getAssignee().getId(),
                                                task.getAssignee() == null
                                                        ? null
                                                        : task.getAssignee().getUsername(),
                                                task.getDueDate(),
                                                task.getCreatedAt(),
                                                task.getUpdatedAt()))
                        .sorted(Comparator.comparing(ProjectTaskSummaryResponse::id))
                        .toList();

        int completedTasksCount =
                (int) project.getTasks().stream()
                        .filter(task -> task.getStatus() == Status.COMPLETED)
                        .count();

        return new ProjectDetailsResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                users.size(),
                tasks.size(),
                completedTasksCount,
                users,
                tasks);
    }
}
