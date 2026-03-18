package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectMemberRequest;
import com.ykleyka.taskboard.dto.ProjectPatchRequest;
import com.ykleyka.taskboard.dto.ProjectRequest;
import com.ykleyka.taskboard.dto.ProjectResponse;
import com.ykleyka.taskboard.dto.ProjectUserSummaryResponse;
import com.ykleyka.taskboard.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService service;

    @GetMapping
    public List<ProjectResponse> getProjects(
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getProjects(pageable);
    }

    @GetMapping("/{id}")
    public ProjectDetailsResponse getProjectById(@PathVariable Long id) {
        return service.getProjectById(id);
    }

    @PostMapping
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
        return service.createProject(request);
    }

    @PostMapping("/{id}/members")
    public ProjectUserSummaryResponse addMember(
            @PathVariable Long id, @Valid @RequestBody ProjectMemberRequest request) {
        return service.addMember(id, request);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return service.updateProject(id, request);
    }

    @PatchMapping("/{id}")
    public ProjectResponse patchProject(
            @PathVariable Long id, @Valid @RequestBody ProjectPatchRequest request) {
        return service.patchProject(id, request);
    }

    @DeleteMapping("/{id}")
    public ProjectResponse deleteProject(@PathVariable Long id) {
        return service.deleteProject(id);
    }
}
