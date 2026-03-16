package com.ykleyka.taskboard.cache;

import com.ykleyka.taskboard.dto.CommentResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommentCache {
    private final Map<CommentPageKey, List<CommentResponse>> cache = new HashMap<>();

    public List<CommentResponse> getByTaskId(CommentPageKey key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }

    public void putByTaskId(CommentPageKey key, List<CommentResponse> comments) {
        synchronized (cache) {
            cache.put(key, comments);
        }
    }

    public void invalidateTask(Long taskId) {
        synchronized (cache) {
            Iterator<CommentPageKey> iterator = cache.keySet().iterator();
            while (iterator.hasNext()) {
                CommentPageKey key = iterator.next();
                if (key.getTaskId().equals(taskId)) {
                    iterator.remove();
                }
            }
        }
    }

    public void invalidate() {
        synchronized (cache) {
            cache.clear();
        }
    }
}
