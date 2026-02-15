package com.ykleyka.taskboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when a task is not found. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TaskNotFoundException extends RuntimeException {

  /** Creates exception for a missing task id. */
  public TaskNotFoundException(Long id) {
    super("Task with id " + id + " not found");
  }
}
