package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.PageKey;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.exception.ProjectConflictException;
import com.ykleyka.taskboard.exception.ProjectNotFoundException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.mapper.ProjectMapper;
import com.ykleyka.taskboard.model.Project;
import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.ProjectMemberId;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.ProjectRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper mapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectCache projectCache;
    private final TagCache tagCache;
    private final CommentCache commentCache;
    private final TaskCache taskCache;

    public List<ProjectResponse> getProjects(Pageable pageable) {
        PageKey key = PageKey.from(pageable);
        List<ProjectResponse> cached = projectCache.getProjects(key);
        if (cached != null) {
            log.info(
                    "Projects returned from cache: page={}, size={}, sort={}",
                    key.getPage(),
                    key.getSize(),
                    key.getSort());
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
            log.info("Project {} details returned from cache", id);
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
        return response;
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setUpdatedAt(Instant.now());
        ProjectResponse response = mapper.toResponse(projectRepository.save(project));
        projectCache.invalidate();
        taskCache.invalidate();
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
            taskCache.invalidate();
        }
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectUserSummaryResponse addMember(Long projectId, ProjectMemberRequest request) {
        if (request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        Project project = findProject(projectId);
        User user = findUser(request.userId());
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ProjectConflictException(
                    "User " + user.getId() + " is already a member of project " + projectId);
        }
        ProjectRole role = request.role() == null ? ProjectRole.MEMBER : request.role();
        ProjectMember member = createMembership(project, user, role);
        projectCache.invalidate();
        return new ProjectUserSummaryResponse(user.getId(), user.getUsername(), member.getRole());
    }

    @Transactional
    public ProjectResponse deleteProject(Long id) {
        Project project = findProject(id);
        List<Task> tasks = taskRepository.findAllByProjectId(id);
        for (Task task : tasks) {
            taskRepository.delete(task);
        }
        projectMemberRepository.deleteAllByProjectId(id);
        projectRepository.delete(project);
        ProjectResponse response = mapper.toResponse(project);
        projectCache.invalidate();
        tagCache.invalidate();
        commentCache.invalidate();
        taskCache.invalidate();
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
        createMembership(project, owner, ProjectRole.OWNER);
    }

    private ProjectMember createMembership(Project project, User user, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setId(new ProjectMemberId(project.getId(), user.getId()));
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        member.setJoinedAt(Instant.now());
        return projectMemberRepository.save(member);
    }
}
