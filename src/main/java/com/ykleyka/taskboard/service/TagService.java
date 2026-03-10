package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.dto.TagRequest;
import com.ykleyka.taskboard.dto.TagResponse;
import com.ykleyka.taskboard.exception.TagNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.mapper.TagMapper;
import com.ykleyka.taskboard.model.Tag;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.repository.TagRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagMapper mapper;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;

    public List<TagResponse> getTags() {
        return tagRepository.findAllWithUsageCount().stream()
                .map(
                        row ->
                                new TagResponse(
                                        row.getId(),
                                        row.getName(),
                                        Math.toIntExact(row.getUsageCount())))
                .toList();
    }

    public TagResponse createTag(TagRequest request) {
        Tag tag = mapper.toEntity(request);
        return mapper.toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse assignTagToTask(Long taskId, Long tagId) {
        Task task = findTask(taskId);
        Tag tag = findTag(tagId);

        boolean hasTag =
                task.getTags().stream().anyMatch(existingTag -> existingTag.getId().equals(tagId));
        if (!hasTag) {
            task.getTags().add(tag);
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
        }
        return mapper.toResponse(tag);
    }

    @Transactional
    public TagResponse removeTagFromTask(Long taskId, Long tagId) {
        Task task = findTask(taskId);
        Tag tag = findTag(tagId);

        boolean removed = task.getTags().removeIf(existingTag -> existingTag.getId().equals(tagId));
        if (removed) {
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
        }
        return mapper.toResponse(tag);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Tag findTag(Long id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }
}
