package com.ykleyka.taskboard.repository;

import com.ykleyka.taskboard.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** In-memory repository for tasks. */
@Repository
public class TaskRepository {
  private final List<Task> tasks = new ArrayList<>();
  private long nextId = 1L;

  /** Returns all tasks. */
  public List<Task> findAll() {
    return new ArrayList<>(tasks);
  }

  /** Finds task by id. */
  public Optional<Task> findById(Long id) {
    for (Task task : tasks) {
      if (task.getId().equals(id)) {
        return Optional.of(task);
      }
    }
    return Optional.empty();
  }

  /** Saves task in repository. */
  public Task save(Task task) {
    if (task.getId() == null) {
      task.setId(nextId++);
    }
    tasks.add(task);
    return task;
  }

  /** Replaces task with matching id. */
  public boolean replace(Long id, Task newTask) {
    for (int i = 0; i < tasks.size(); i++) {
      if (tasks.get(i).getId().equals(id)) {
        tasks.set(i, newTask);
        return true;
      }
    }
    return false;
  }

  /** Deletes task by id. */
  public boolean deleteById(Long id) {
    return tasks.removeIf(task -> task.getId().equals(id));
  }
}
