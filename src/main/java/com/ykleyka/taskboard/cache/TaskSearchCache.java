package com.ykleyka.taskboard.cache;

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
public class TaskSearchCache {
    private final Map<TaskSearchKey, List<TaskResponse>> cache = new HashMap<>();

    public List<TaskResponse> get(TaskSearchKey key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    public void put(TaskSearchKey key, List<TaskResponse> value) {
        synchronized (cache) {
            cache.put(key, value);
        }
    }

    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static final class TaskSearchKey {
        private final Long projectId;
        private final String tagName;
        private final Status status;
        private final String assignee;
        private final Instant dueBefore;
        private final int page;
        private final int size;
        private final String sort;
        private final boolean nativeQuery;

        public static TaskSearchKey from(
                Long projectId,
                String tagName,
                Status status,
                String assignee,
                Instant dueBefore,
                Pageable pageable,
                boolean nativeQuery) {
            String sortValue = pageable.getSort() == null ? "" : pageable.getSort().toString();
            return new TaskSearchKey(
                    projectId,
                    tagName,
                    status,
                    assignee,
                    dueBefore,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sortValue,
                    nativeQuery);
        }
    }
}
