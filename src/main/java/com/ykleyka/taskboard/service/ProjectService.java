package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.ProjectCreateRequest;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.ProjectMapper;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectMapper mapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectMapper mapper,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.mapper = mapper;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public List<ProjectResponse> getProjects() {
        return projectRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    public ProjectDetailsResponse getProjectById(Long id) {
        return mapper.toDetailsResponse(findDetailedProject(id));
    }

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        User owner = findUser(request.ownerId());
        Project project = mapper.toEntity(request);
        Project savedProject = projectRepository.save(project);
        createOwnerMembership(savedProject, owner);
        return mapper.toResponse(savedProject);
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setUpdatedAt(Instant.now());
        return mapper.toResponse(projectRepository.save(project));
    }

    public ProjectResponse patchProject(Long id, ProjectPatchRequest request) {
        Project project = findProject(id);
        boolean changed = false;
        if (request.name() != null) {
            project.setName(request.name());
            changed = true;
        }
        if (request.description() != null) {
            project.setDescription(request.description());
            changed = true;
        }
        if (changed) {
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
        }
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse deleteProject(Long id) {
        Project project = findProject(id);
        taskRepository.deleteAllByProjectId(id);
        projectMemberRepository.deleteAllByProjectId(id);
        projectRepository.delete(project);
        return mapper.toResponse(project);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private Project findDetailedProject(Long id) {
        return projectRepository.findDetailedById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void createOwnerMembership(Project project, User owner) {
        ProjectMember member = new ProjectMember();
        member.setId(new ProjectMemberId(project.getId(), owner.getId()));
        member.setProject(project);
        member.setUser(owner);
        member.setRole(ProjectRole.OWNER);
        member.setJoinedAt(Instant.now());
        projectMemberRepository.save(member);
    }
}
