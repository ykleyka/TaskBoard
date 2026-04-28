package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.CommentCache;
import com.ykleyka.taskboard.cache.ProjectCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.UserPatchRequest;
import com.ykleyka.taskboard.exception.UserConflictException;
import com.ykleyka.taskboard.exception.UserNotFoundException;
import com.ykleyka.taskboard.model.ProjectMember;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.model.enums.ProjectRole;
import com.ykleyka.taskboard.repository.CommentRepository;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import com.ykleyka.taskboard.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final CommentRepository commentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectCache projectCache;
    private final CommentCache commentCache;
    private final TaskCache taskCache;

    public List<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).getContent();
    }

    public List<User> getUsers(Long currentUserId, Pageable pageable) {
        return userRepository.findAllVisibleToUser(currentUserId, pageable).getContent();
    }

    public User getUserById(Long id) {
        return findUser(id);
    }

    public User getUserById(Long id, Long currentUserId) {
        return userRepository
                .findVisibleToUserById(id, currentUserId)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User createUser(User user) {
        validateUsernameAndEmail(user.getUsername(), user.getEmail());
        user.setPasswordHash(encodePassword(user.getPasswordHash()));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    public User updateUser(Long id, User request) {
        User user = findUser(id);
        validateUsernameAndEmailForUpdate(id, request.getUsername(), request.getEmail());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPasswordHash() != null) {
            user.setPasswordHash(encodePassword(request.getPasswordHash()));
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        commentCache.invalidate();
        projectCache.invalidate();
        taskCache.invalidate();
        return saved;
    }

    public User patchUser(Long id, UserPatchRequest request) {
        User user = findUser(id);
        String username = request.username();
        String email = request.email();
        String passwordHash = request.password();
        String firstName = request.firstName();
        String lastName = request.lastName();
        if (username != null && !username.equalsIgnoreCase(user.getUsername())) {
            validateUsernameAndEmailForUpdate(id, username, user.getEmail());
            user.setUsername(username);
        }
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            validateUsernameAndEmailForUpdate(id, user.getUsername(), email);
            user.setEmail(email);
        }
        if (passwordHash != null) {
            user.setPasswordHash(encodePassword(passwordHash));
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        commentCache.invalidate();
        projectCache.invalidate();
        taskCache.invalidate();
        return saved;
    }

    @Transactional
    public User deleteUser(Long id) {
        User user = findUser(id);
        List<Task> affectedTasks = taskRepository.findAllByCreatorIdOrAssigneeId(id, id);
        Map<Long, User> replacementOwners = loadReplacementOwnersByProjectId(affectedTasks, id);
        Instant now = Instant.now();
        for (Task task : affectedTasks) {
            boolean changed = false;

            if (task.getCreator() != null && task.getCreator().getId().equals(id)) {
                task.setCreator(replacementOwners.get(task.getProject().getId()));
                changed = true;
            }
            if (task.getAssignee() != null && task.getAssignee().getId().equals(id)) {
                task.setAssignee(task.getCreator());
                changed = true;
            }
            if (changed) {
                task.setUpdatedAt(now);
            }
        }
        if (!affectedTasks.isEmpty()) {
            taskRepository.saveAll(affectedTasks);
        }
        commentRepository.deleteAllByAuthorId(id);
        commentCache.invalidate();
        projectMemberRepository.deleteAllByUserId(id);
        userRepository.delete(user);
        projectCache.invalidate();
        taskCache.invalidate();
        return user;
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validateUsernameAndEmail(String username, String email) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserConflictException("User with username " + username + " already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserConflictException("User with email " + email + " already exists");
        }
    }

    private void validateUsernameAndEmailForUpdate(Long id, String username, String email) {
        userRepository
                .findByUsernameIgnoreCase(username)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserConflictException(
                            "User with username " + username + " already exists");
                });

        userRepository
                .findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new UserConflictException("User with email " + email + " already exists");
                });
    }

    private String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not be blank");
        }
        return passwordEncoder.encode(rawPassword);
    }

    private Map<Long, User> loadReplacementOwnersByProjectId(
            List<Task> affectedTasks, Long deletedUserId) {
        Set<Long> projectIds = new HashSet<>();
        List<ProjectMember> ownedProjects =
                projectMemberRepository.findAllByUserIdAndRole(deletedUserId, ProjectRole.OWNER);
        for (ProjectMember membership : ownedProjects) {
            projectIds.add(membership.getProject().getId());
        }
        for (Task task : affectedTasks) {
            if (task.getCreator() == null || !task.getCreator().getId().equals(deletedUserId)) {
                continue;
            }
            if (task.getProject() == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete user "
                                + deletedUserId
                                + " because task "
                                + task.getId()
                                + " has no project");
            }
            projectIds.add(task.getProject().getId());
        }

        if (projectIds.isEmpty()) {
            return Map.of();
        }

        List<ProjectMember> owners =
                projectMemberRepository.findAllByProjectIdInAndRoleAndUserIdNot(
                        projectIds, ProjectRole.OWNER, deletedUserId);
        Map<Long, User> ownersByProjectId = new HashMap<>();
        for (ProjectMember owner : owners) {
            ownersByProjectId.putIfAbsent(owner.getProject().getId(), owner.getUser());
        }

        for (Long projectId : projectIds) {
            if (ownersByProjectId.containsKey(projectId)) {
                continue;
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete user "
                            + deletedUserId
                            + " because project "
                            + projectId
                            + " has no owner");
        }
        return ownersByProjectId;
    }
}
