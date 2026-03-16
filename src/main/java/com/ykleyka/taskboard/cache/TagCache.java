package com.ykleyka.taskboard.cache;

import com.ykleyka.taskboard.dto.TagResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TagCache {
    private final Map<PageKey, List<TagResponse>> listCache = new HashMap<>();

    public List<TagResponse> getTags(PageKey key) {
        synchronized (listCache) {
            return listCache.get(key);
        }
    }

    public void putTags(PageKey key, List<TagResponse> tags) {
        synchronized (listCache) {
            listCache.put(key, tags);
        }
    }

    public void invalidate() {
        synchronized (listCache) {
            listCache.clear();
        }
    }
}
