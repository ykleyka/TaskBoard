package com.ykleyka.taskboard.cache;

import com.ykleyka.taskboard.dto.ProjectDetailsResponse;
import com.ykleyka.taskboard.dto.ProjectResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProjectCache {
    private final Map<PageKey, List<ProjectResponse>> listCache = new HashMap<>();
    private final Map<Long, ProjectDetailsResponse> detailsCache = new HashMap<>();

    public List<ProjectResponse> getProjects(PageKey key) {
        synchronized (listCache) {
            return listCache.get(key);
        }
    }

    public void putProjects(PageKey key, List<ProjectResponse> projects) {
        synchronized (listCache) {
            listCache.put(key, projects);
        }
    }

    public ProjectDetailsResponse getProjectDetails(Long projectId) {
        synchronized (detailsCache) {
            return detailsCache.get(projectId);
        }
    }

    public void putProjectDetails(Long projectId, ProjectDetailsResponse response) {
        synchronized (detailsCache) {
            detailsCache.put(projectId, response);
        }
    }

    public void invalidate() {
        synchronized (listCache) {
            listCache.clear();
        }
        synchronized (detailsCache) {
            detailsCache.clear();
        }
    }
}
