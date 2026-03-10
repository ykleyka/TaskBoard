package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.TagRequest;
import com.ykleyka.taskboard.dto.TagResponse;
import com.ykleyka.taskboard.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class TagController {
    private final TagService service;

    @GetMapping("/tags")
    public List<TagResponse> getTags() {
        return service.getTags();
    }

    @PostMapping("/tags")
    public TagResponse createTag(@Valid @RequestBody TagRequest request) {
        return service.createTag(request);
    }

    @PostMapping("/tasks/{taskId}/tags/{tagId}")
    public TagResponse assignTagToTask(@PathVariable Long taskId, @PathVariable Long tagId) {
        return service.assignTagToTask(taskId, tagId);
    }

    @DeleteMapping("/tasks/{taskId}/tags/{tagId}")
    public TagResponse removeTagFromTask(@PathVariable Long taskId, @PathVariable Long tagId) {
        return service.removeTagFromTask(taskId, tagId);
    }
}
