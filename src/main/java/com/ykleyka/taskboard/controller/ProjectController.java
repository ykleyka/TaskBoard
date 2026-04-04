package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectMemberRoleRequest;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.service.ProjectService;
import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Operations for managing projects and project members")
public class ProjectController {
    private final ProjectService service;

    @Operation(summary = "List projects", description = "Returns a paginated list of projects.")
    @GetMapping
    public List<ProjectResponse> getProjects(
            @ParameterObject @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getProjects(pageable);
    }

    @Operation(summary = "Get project by id", description = "Returns detailed project information.")
    @GetMapping("/{id}")
    public ProjectDetailsResponse getProjectById(@PathVariable @Positive Long id) {
        return service.getProjectById(id);
    }

    @Operation(summary = "List project members", description = "Returns members of the specified project.")
    @GetMapping("/{id}/members")
    public List<ProjectUserSummaryResponse> getProjectMembers(@PathVariable @Positive Long id) {
        return service.getProjectMembers(id);
    }

    @Operation(summary = "Get project member", description = "Returns project member by user id.")
    @GetMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse getProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId) {
        return service.getProjectMember(id, userId);
    }

    @Operation(summary = "Create project", description = "Creates a new project.")
    @PostMapping
    public ProjectResponse createProject(
            @Validated({Default.class, OnCreate.class}) @RequestBody ProjectRequest request) {
        return service.createProject(request);
    }

    @Operation(summary = "Add project member", description = "Adds a user to the specified project.")
    @PostMapping("/{id}/members")
    public ProjectUserSummaryResponse addMember(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProjectMemberRequest request) {
        return service.addMember(id, request);
    }

    @Operation(summary = "Update project member", description = "Updates role of the project member.")
    @PutMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse updateProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ProjectMemberRoleRequest request) {
        return service.updateProjectMember(id, userId, request);
    }

    @Operation(
            summary = "Bulk add project members",
            description =
                    "Adds multiple users to the specified project in a single transaction. "
                            + "If one element fails, all changes are rolled back.")
    @PostMapping("/{id}/members/bulk")
    public List<ProjectUserSummaryResponse> addMembersBulk(
            @PathVariable @Positive Long id,
            @RequestBody @NotEmpty List<@Valid ProjectMemberRequest> requests) {
        return service.addMembersBulk(id, requests);
    }

    @Operation(summary = "Replace project", description = "Fully updates an existing project.")
    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable @Positive Long id, @Valid @RequestBody ProjectRequest request) {
        return service.updateProject(id, request);
    }

    @Operation(summary = "Patch project", description = "Partially updates an existing project.")
    @PatchMapping("/{id}")
    public ProjectResponse patchProject(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProjectPatchRequest request) {
        return service.patchProject(id, request);
    }

    @Operation(summary = "Delete project member", description = "Removes a user from the specified project.")
    @DeleteMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse deleteProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId) {
        return service.deleteProjectMember(id, userId);
    }

    @Operation(summary = "Delete project", description = "Deletes a project and returns the removed entity.")
    @DeleteMapping("/{id}")
    public ProjectResponse deleteProject(@PathVariable @Positive Long id) {
        return service.deleteProject(id);
    }
}
