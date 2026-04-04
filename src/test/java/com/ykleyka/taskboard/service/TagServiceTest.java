package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.cache.TagCache;
import com.ykleyka.taskboard.cache.TaskCache;
import com.ykleyka.taskboard.dto.TagRequest;
import com.ykleyka.taskboard.dto.TagResponse;
import com.ykleyka.taskboard.exception.TagNotFoundException;
import com.ykleyka.taskboard.exception.TaskNotFoundException;
import com.ykleyka.taskboard.mapper.TagMapper;
import com.ykleyka.taskboard.model.Tag;
import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.repository.TagRepository;
import com.ykleyka.taskboard.repository.TaskRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    @Mock
    private TagMapper mapper;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TagCache tagCache;
    @Mock
    private TaskCache taskCache;

    @InjectMocks
    private TagService service;

    @Test
    void getTags_whenCacheHit_returnsCachedList() {
        Pageable pageable = PageRequest.of(0, 20);
        List<TagResponse> cached = List.of(new TagResponse(1L, "backend", 3));
        when(tagCache.getTags(any())).thenReturn(cached);

        List<TagResponse> actual = service.getTags(pageable);

        assertEquals(cached, actual);
        verify(tagRepository, never()).findAllWithUsageCount(any(Pageable.class));
    }

    @Test
    void getTags_whenCacheMiss_mapsProjectionAndCaches() {
        Pageable pageable = PageRequest.of(0, 20);
        TagRepository.TagUsageProjection projection = projection(5L, "urgent", 7L);
        when(tagCache.getTags(any())).thenReturn(null);
        when(tagRepository.findAllWithUsageCount(pageable)).thenReturn(new PageImpl<>(List.of(projection)));

        List<TagResponse> actual = service.getTags(pageable);

        assertEquals(1, actual.size());
        assertEquals(5L, actual.get(0).id());
        assertEquals("urgent", actual.get(0).name());
        assertEquals(7, actual.get(0).usageCount());
        verify(tagCache).putTags(any(), any());
    }

    @Test
    void createTag_whenValid_savesAndInvalidatesCache() {
        TagRequest request = new TagRequest("backend");
        Tag entity = tag(1L, "backend");
        TagResponse expected = new TagResponse(1L, "backend", 0);

        when(mapper.toEntity(request)).thenReturn(entity);
        when(tagRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(expected);

        TagResponse actual = service.createTag(request);

        assertEquals(expected, actual);
        verify(tagCache).invalidate();
    }

    @Test
    void assignTagToTask_whenTaskMissing_throwsTaskNotFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.assignTagToTask(10L, 20L));
    }

    @Test
    void assignTagToTask_whenTagMissing_throwsTagNotFound() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L)));
        when(tagRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class, () -> service.assignTagToTask(10L, 20L));
    }

    @Test
    void assignTagToTask_whenTagIsNotAssigned_addsTagAndInvalidatesCaches() {
        Long taskId = 10L;
        Long tagId = 20L;
        Task task = task(taskId);
        Tag tag = tag(tagId, "backend");
        TagResponse expected = new TagResponse(tagId, "backend", 1);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(tag)).thenReturn(expected);

        TagResponse actual = service.assignTagToTask(taskId, tagId);

        assertEquals(expected, actual);
        assertTrue(task.getTags().contains(tag));
        assertNotNull(task.getUpdatedAt());
        verify(taskRepository).save(task);
        verify(tagCache).invalidate();
        verify(taskCache).invalidateTask(taskId);
    }

    @Test
    void assignTagToTask_whenTagAlreadyAssigned_doesNotSaveTask() {
        Long taskId = 30L;
        Long tagId = 40L;
        Task task = task(taskId);
        Tag tag = tag(tagId, "urgent");
        task.getTags().add(tag);
        TagResponse expected = new TagResponse(tagId, "urgent", 1);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(mapper.toResponse(tag)).thenReturn(expected);

        TagResponse actual = service.assignTagToTask(taskId, tagId);

        assertEquals(expected, actual);
        verify(taskRepository, never()).save(any(Task.class));
        verify(tagCache, never()).invalidate();
        verify(taskCache, never()).invalidateTask(taskId);
    }

    @Test
    void removeTagFromTask_whenRemoved_savesAndInvalidatesCaches() {
        Long taskId = 50L;
        Long tagId = 60L;
        Task task = task(taskId);
        Tag tag = tag(tagId, "ops");
        task.getTags().add(tag);
        TagResponse expected = new TagResponse(tagId, "ops", 1);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(tag)).thenReturn(expected);

        TagResponse actual = service.removeTagFromTask(taskId, tagId);

        assertEquals(expected, actual);
        assertNotNull(task.getUpdatedAt());
        verify(taskRepository).save(task);
        verify(tagCache).invalidate();
        verify(taskCache).invalidateTask(taskId);
    }

    @Test
    void removeTagFromTask_whenTagNotAssigned_doesNotSave() {
        Long taskId = 70L;
        Long tagId = 80L;
        Task task = task(taskId);
        Tag tag = tag(tagId, "not-assigned");
        TagResponse expected = new TagResponse(tagId, "not-assigned", 0);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(mapper.toResponse(tag)).thenReturn(expected);

        TagResponse actual = service.removeTagFromTask(taskId, tagId);

        assertEquals(expected, actual);
        verify(taskRepository, never()).save(any(Task.class));
        verify(tagCache, never()).invalidate();
        verify(taskCache, never()).invalidateTask(taskId);
    }

    private Task task(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setTags(new HashSet<>());
        return task;
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private TagRepository.TagUsageProjection projection(Long id, String name, long usage) {
        return new TagRepository.TagUsageProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getUsageCount() {
                return usage;
            }
        };
    }
}
