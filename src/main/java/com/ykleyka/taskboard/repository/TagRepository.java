package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    @Query("""
            select t.id as id, t.name as name, count(task.id) as usageCount
            from Tag t
            left join t.tasks task
            group by t.id, t.name
            order by t.id
            """)
    List<TagUsageProjection> findAllWithUsageCount();

    interface TagUsageProjection {
        Long getId();

        String getName();

        long getUsageCount();
    }
}
