package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.AsyncTaskSubmissionResponse;
import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectMemberRoleRequest;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.service.ProjectService;
import com.ykleyka.taskboard.service.ProjectSummaryReportTaskService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final ProjectSummaryReportTaskService projectSummaryReportTaskService;

    @Operation(summary = "List projects", description = "Returns a paginated list of projects.")
    @GetMapping
    public List<ProjectResponse> getProjects(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @ParameterObject @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getProjects(currentUser.id(), pageable);
    }

    @Operation(summary = "Get project by id", description = "Returns detailed project information.")
    @GetMapping("/{id}")
    public ProjectDetailsResponse getProjectById(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getProjectById(id, currentUser.id());
    }

    @Operation(summary = "List project members", description = "Returns members of the specified project.")
    @GetMapping("/{id}/members")
    public List<ProjectUserSummaryResponse> getProjectMembers(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getProjectMembers(id, currentUser.id());
    }

    @Operation(summary = "Get project member", description = "Returns project member by user id.")
    @GetMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse getProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getProjectMember(id, userId, currentUser.id());
    }

    @Operation(summary = "Create project", description = "Creates a new project.")
    @PostMapping
    public ProjectResponse createProject(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Validated({Default.class, OnCreate.class}) @RequestBody ProjectRequest request) {
        return service.createProject(request, currentUser.id());
    }

    @Operation(
            summary = "Generate project summary report asynchronously",
            description =
                    "Starts asynchronous generation of the project summary report and returns "
                            + "the async task id.")
    @PostMapping("/{id}/summary-report")
    public ResponseEntity<AsyncTaskSubmissionResponse> generateProjectSummaryReport(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.requireProjectMember(id, currentUser.id());
        return ResponseEntity.accepted()
                .body(projectSummaryReportTaskService.submitProjectSummaryReport(id, currentUser.id()));
    }

    @Operation(summary = "Add project member", description = "Adds a user to the specified project.")
    @PostMapping("/{id}/members")
    public ProjectUserSummaryResponse addMember(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProjectMemberRequest request) {
        return service.addMember(id, request, currentUser.id());
    }

    @Operation(summary = "Update project member", description = "Updates role of the project member.")
    @PutMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse updateProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProjectMemberRoleRequest request) {
        return service.updateProjectMember(id, userId, request, currentUser.id());
    }

    @Operation(
            summary = "Bulk add project members",
            description =
                    "Adds multiple users to the specified project in a single transaction. "
                            + "If one element fails, all changes are rolled back.")
    @PostMapping("/{id}/members/bulk")
    public List<ProjectUserSummaryResponse> addMembersBulk(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestBody @NotEmpty List<@Valid ProjectMemberRequest> requests) {
        return service.addMembersBulk(id, requests, currentUser.id());
    }

    @Operation(summary = "Replace project", description = "Fully updates an existing project.")
    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProjectRequest request) {
        return service.updateProject(id, request, currentUser.id());
    }

    @Operation(summary = "Patch project", description = "Partially updates an existing project.")
    @PatchMapping("/{id}")
    public ProjectResponse patchProject(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ProjectPatchRequest request) {
        return service.patchProject(id, request, currentUser.id());
    }

    @Operation(summary = "Delete project member", description = "Removes a user from the specified project.")
    @DeleteMapping("/{id}/members/{userId}")
    public ProjectUserSummaryResponse deleteProjectMember(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.deleteProjectMember(id, userId, currentUser.id());
    }

    @Operation(summary = "Delete project", description = "Deletes a project and returns the removed entity.")
    @DeleteMapping("/{id}")
    public ProjectResponse deleteProject(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.deleteProject(id, currentUser.id());
    }
}
