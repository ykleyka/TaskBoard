package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TaskSearchCache;
import com.ykleyka.taskboard.cache.PageKey;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper mapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectCache projectCache;
    private final TaskSearchCache searchCache;

    public List<ProjectResponse> getProjects(Pageable pageable) {
        PageKey key = PageKey.from(pageable);
        List<ProjectResponse> cached = projectCache.getProjects(key);
        if (cached != null) {
            return cached;
        }
        List<ProjectResponse> content =
                projectRepository.findAll(pageable).map(mapper::toResponse).getContent();
        projectCache.putProjects(key, content);
        return content;
    }

    public ProjectDetailsResponse getProjectById(Long id) {
        ProjectDetailsResponse cached = projectCache.getProjectDetails(id);
        if (cached != null) {
            return cached;
        }
        ProjectDetailsResponse response = mapper.toDetailsResponse(findDetailedProject(id));
        projectCache.putProjectDetails(id, response);
        return response;
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        if (request.ownerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerId is required");
        }
        User owner = findUser(request.ownerId());
        Project project = mapper.toEntity(request);
        Project savedProject = projectRepository.save(project);
        createOwnerMembership(savedProject, owner);
        ProjectResponse response = mapper.toResponse(savedProject);
        projectCache.invalidate();
        searchCache.invalidate();
        return response;
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setUpdatedAt(Instant.now());
        ProjectResponse response = mapper.toResponse(projectRepository.save(project));
        projectCache.invalidate();
        searchCache.invalidate();
        return response;
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
            projectCache.invalidate();
            searchCache.invalidate();
        }
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse deleteProject(Long id) {
        Project project = findProject(id);
        taskRepository.deleteAllByProjectId(id);
        projectMemberRepository.deleteAllByProjectId(id);
        projectRepository.delete(project);
        ProjectResponse response = mapper.toResponse(project);
        projectCache.invalidate();
        searchCache.invalidate();
        return response;
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
