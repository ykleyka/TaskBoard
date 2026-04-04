package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.PageKey;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectMemberRoleRequest;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<ProjectUserSummaryResponse> getProjectMembers(Long id) {
        findProject(id);
        return projectMemberRepository.findAllByProjectId(id).stream()
                .map(this::toProjectUserSummary)
                .sorted(Comparator.comparing(ProjectUserSummaryResponse::id))
                .toList();
    }

    public ProjectUserSummaryResponse getProjectMember(Long projectId, Long userId) {
        findProject(projectId);
        return toProjectUserSummary(findProjectMember(projectId, userId));
    }

    @Transactional
    public ProjectUserSummaryResponse updateProjectMember(
            Long projectId, Long userId, ProjectMemberRoleRequest request) {
        findProject(projectId);
        ProjectMember member = findProjectMember(projectId, userId);
        member.setRole(request.role());
        ProjectMember saved = projectMemberRepository.save(member);
        projectCache.invalidate();
        return toProjectUserSummary(saved);
    }

    @Transactional
    public ProjectUserSummaryResponse deleteProjectMember(Long projectId, Long userId) {
        findProject(projectId);
        ProjectMember member = findProjectMember(projectId, userId);
        projectMemberRepository.delete(member);
        projectCache.invalidate();
        taskCache.invalidate();
        return toProjectUserSummary(member);
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
        Project project = findProject(projectId);
        ProjectUserSummaryResponse response = addMemberInternal(project, projectId, request);
        projectCache.invalidate();
        return response;
    }

    @Transactional
    public List<ProjectUserSummaryResponse> addMembersBulk(
            Long projectId, List<ProjectMemberRequest> requests) {
        Project project = findProject(projectId);
        try {
            return addMembersBulkInternal(project, projectId, requests);
        } finally {
            projectCache.invalidate();
        }
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

    private List<ProjectUserSummaryResponse> addMembersBulkInternal(
            Project project, Long projectId, List<ProjectMemberRequest> requests) {
        List<ProjectMemberRequest> validatedRequests = requireBulkRequests(requests);
        ensureNoDuplicateUserIds(validatedRequests);
        return validatedRequests.stream()
                .map(request -> addMemberInternal(project, projectId, request))
                .toList();
    }

    private ProjectUserSummaryResponse addMemberInternal(
            Project project, Long projectId, ProjectMemberRequest request) {
        Long userId = requireUserId(request);
        User user = findUser(userId);
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ProjectConflictException(
                    "User " + userId + " is already a member of project " + projectId);
        }
        ProjectRole role = Optional.ofNullable(request.role()).orElse(ProjectRole.MEMBER);
        ProjectMember member = createMembership(project, user, role);
        return new ProjectUserSummaryResponse(user.getId(), user.getUsername(), member.getRole());
    }

    private List<ProjectMemberRequest> requireBulkRequests(List<ProjectMemberRequest> requests) {
        return Optional.ofNullable(requests)
                .filter(list -> !list.isEmpty())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "members list must not be empty"));
    }

    private void ensureNoDuplicateUserIds(List<ProjectMemberRequest> requests) {
        Set<Long> duplicateUserIds =
                requests.stream()
                        .map(this::requireUserId)
                        .collect(Collectors.groupingBy(userId -> userId, Collectors.counting()))
                        .entrySet().stream()
                        .filter(entry -> entry.getValue() > 1)
                        .map(Entry::getKey)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!duplicateUserIds.isEmpty()) {
            throw new ProjectConflictException(
                    "Request contains duplicate userIds: " + duplicateUserIds);
        }
    }

    private Long requireUserId(ProjectMemberRequest request) {
        return Optional.ofNullable(request)
                .map(ProjectMemberRequest::userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "userId is required"));
    }

    private ProjectMember findProjectMember(Long projectId, Long userId) {
        return projectMemberRepository
                .findById(new ProjectMemberId(projectId, userId))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Member with userId " + userId + " not found in project " + projectId));
    }

    private ProjectUserSummaryResponse toProjectUserSummary(ProjectMember member) {
        return new ProjectUserSummaryResponse(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole());
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
