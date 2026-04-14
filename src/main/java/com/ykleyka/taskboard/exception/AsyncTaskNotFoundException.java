package com.ykleyka.taskboard.exception;

public class AsyncTaskNotFoundException extends RuntimeException {
    public AsyncTaskNotFoundException(String taskId) {
        super("Async task with id " + taskId + " not found");
    }
}
