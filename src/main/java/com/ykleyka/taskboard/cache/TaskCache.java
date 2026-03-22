package com.ykleyka.taskboard.cache;

import com.ykleyka.taskboard.dto.TaskDetailsResponse;
import com.ykleyka.taskboard.dto.TaskResponse;
import com.ykleyka.taskboard.model.enums.Status;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TaskCache {
    private final Map<Long, TaskDetailsResponse> detailsCache = new HashMap<>();
    private final Map<TaskQueryKey, List<TaskResponse>> queryCache = new HashMap<>();

    public TaskDetailsResponse getTaskDetails(Long taskId) {
        synchronized (detailsCache) {
            return detailsCache.get(taskId);
        }
    }

    public void putTaskDetails(Long taskId, TaskDetailsResponse response) {
        synchronized (detailsCache) {
            detailsCache.put(taskId, response);
        }
    }

    public List<TaskResponse> getQuery(TaskQueryKey key) {
        synchronized (queryCache) {
            return queryCache.get(key);
        }
    }

    public void putQuery(TaskQueryKey key, List<TaskResponse> response) {
        synchronized (queryCache) {
            queryCache.put(key, response);
        }
    }

    public void invalidateTask(Long taskId) {
        invalidateTaskDetails(taskId);
        invalidateQueries();
    }

    public void invalidateTaskDetails(Long taskId) {
        synchronized (detailsCache) {
            detailsCache.remove(taskId);
        }
    }

    public void invalidateQueries() {
        synchronized (queryCache) {
            queryCache.clear();
        }
    }

    public void invalidate() {
        synchronized (detailsCache) {
            detailsCache.clear();
        }
        synchronized (queryCache) {
            queryCache.clear();
        }
    }

    public enum TaskQueryType {
        LIST,
        SEARCH,
        OVERDUE
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static final class TaskQueryKey {
        private final TaskQueryType queryType;
        private final Long projectId;
        private final String tagName;
        private final Status status;
        private final String assignee;
        private final Instant dueBefore;
        private final int page;
        private final int size;
        private final String sort;

        public static TaskQueryKey from(
                TaskQueryType queryType,
                Long projectId,
                String tagName,
                Status status,
                String assignee,
                Instant dueBefore,
                Pageable pageable) {
            String sortValue = pageable.getSort() == null ? "" : pageable.getSort().toString();
            return new TaskQueryKey(
                    queryType,
                    projectId,
                    tagName,
                    status,
                    assignee,
                    dueBefore,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sortValue);
        }
    }
}
