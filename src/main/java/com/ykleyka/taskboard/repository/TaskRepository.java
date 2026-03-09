package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Task;
import com.ykleyka.taskboard.model.enums.Status;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByCreatorIdOrAssigneeId(Long creatorId, Long assigneeId);

    List<Task> findAllByStatus(Status status, Sort sort);

    List<Task> findAllByAssigneeUsernameIgnoreCase(String assignee, Sort sort);

    List<Task> findAllByStatusAndAssigneeUsernameIgnoreCase(
            Status status, String assignee, Sort sort);
}
