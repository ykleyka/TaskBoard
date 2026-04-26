package com.ykleyka.taskboard.service;

import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.PageKey;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.TagRequest;
import com.ykleyka.taskboard.dto.TagResponse;
import com.ykleyka.taskboard.exception.TagNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.mapper.TagMapper;
import com.ykleyka.taskboard.model.Tag;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.repository.ProjectMemberRepository;
import com.ykleyka.taskboard.repository.TagRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
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
public class TagService {
    private final TagMapper mapper;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TagCache tagCache;
    private final TaskCache taskCache;

    public List<TagResponse> getTags(Pageable pageable) {
        PageKey key = PageKey.from(pageable);
        List<TagResponse> cached = tagCache.getTags(key);
        if (cached != null) {
            log.info(
                    "Tags returned from cache: page={}, size={}, sort={}",
                    key.getPage(),
                    key.getSize(),
                    key.getSort());
            return cached;
        }
        List<TagResponse> content =
                tagRepository.findAllWithUsageCount(pageable)
                        .map(
                                row ->
                                        new TagResponse(
                                                row.getId(),
                                                row.getName(),
                                                Math.toIntExact(row.getUsageCount())))
                        .getContent();
        tagCache.putTags(key, content);
        return content;
    }

    public TagResponse createTag(TagRequest request) {
        Tag tag = mapper.toEntity(request);
        TagResponse response = mapper.toResponse(tagRepository.save(tag));
        tagCache.invalidate();
        return response;
    }

    @Transactional
    public TagResponse assignTagToTask(Long taskId, Long tagId, Long currentUserId) {
        requireTaskMember(findTask(taskId), currentUserId);
        return assignTagToTask(taskId, tagId);
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
            tagCache.invalidate();
            taskCache.invalidateTask(taskId);
        }
        return mapper.toResponse(tag);
    }

    @Transactional
    public TagResponse removeTagFromTask(Long taskId, Long tagId, Long currentUserId) {
        requireTaskMember(findTask(taskId), currentUserId);
        return removeTagFromTask(taskId, tagId);
    }

    @Transactional
    public TagResponse removeTagFromTask(Long taskId, Long tagId) {
        Task task = findTask(taskId);
        Tag tag = findTag(tagId);

        boolean removed = task.getTags().removeIf(existingTag -> existingTag.getId().equals(tagId));
        if (removed) {
            task.setUpdatedAt(Instant.now());
            taskRepository.save(task);
            tagCache.invalidate();
            taskCache.invalidateTask(taskId);
        }
        return mapper.toResponse(tag);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Tag findTag(Long id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }

    private void requireTaskMember(Task task, Long userId) {
        if (task == null || task.getProject() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task has no project");
        }
        Long projectId = task.getProject().getId();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
    }
}
