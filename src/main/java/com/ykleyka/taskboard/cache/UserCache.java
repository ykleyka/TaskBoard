package com.ykleyka.taskboard.cache;

import com.ykleyka.taskboard.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UserCache {
    private final Map<PageKey, List<User>> listCache = new HashMap<>();
    private final Map<Long, User> userCacheMap = new HashMap<>();

    public List<User> getUsers(PageKey key) {
        synchronized (listCache) {
            return listCache.get(key);
        }
    }

    public void putUsers(PageKey key, List<User> users) {
        synchronized (listCache) {
            listCache.put(key, users);
        }
    }

    public User getUser(Long id) {
        synchronized (userCacheMap) {
            return userCacheMap.get(id);
        }
    }

    public void putUser(Long id, User user) {
        synchronized (userCacheMap) {
            userCacheMap.put(id, user);
        }
    }

    public void invalidate() {
        synchronized (listCache) {
            listCache.clear();
        }
        synchronized (userCacheMap) {
            userCacheMap.clear();
        }
    }
}
