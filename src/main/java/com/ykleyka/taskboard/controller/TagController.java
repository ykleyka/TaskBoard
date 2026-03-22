package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.TagRequest;
import com.ykleyka.taskboard.dto.TagResponse;
import com.ykleyka.taskboard.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Operations for managing tags and task-tag assignments")
public class TagController {
    private final TagService service;

    @Operation(summary = "List tags", description = "Returns a paginated list of tags.")
    @GetMapping("/tags")
    public List<TagResponse> getTags(
            @ParameterObject @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getTags(pageable);
    }

    @Operation(summary = "Create tag", description = "Creates a new tag.")
    @PostMapping("/tags")
    public TagResponse createTag(@Valid @RequestBody TagRequest request) {
        return service.createTag(request);
    }

    @Operation(summary = "Assign tag to task", description = "Links an existing tag to a task.")
    @PostMapping("/tasks/{taskId}/tags/{tagId}")
    public TagResponse assignTagToTask(
            @PathVariable @Positive Long taskId,
            @PathVariable @Positive Long tagId) {
        return service.assignTagToTask(taskId, tagId);
    }

    @Operation(summary = "Remove tag from task", description = "Removes a tag from a task.")
    @DeleteMapping("/tasks/{taskId}/tags/{tagId}")
    public TagResponse removeTagFromTask(
            @PathVariable @Positive Long taskId,
            @PathVariable @Positive Long tagId) {
        return service.removeTagFromTask(taskId, tagId);
    }
}
