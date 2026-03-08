package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Task;
import java.util.List;
import com.ykleyka.taskboard.model.enums.Status;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByStatus(Status status, Sort sort);

    List<Task> findAllByAssigneeIgnoreCase(String assignee, Sort sort);

    List<Task> findAllByStatusAndAssigneeIgnoreCase(Status status, String assignee, Sort sort);
}
