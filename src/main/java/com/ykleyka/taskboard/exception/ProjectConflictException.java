package com.ykleyka.taskboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProjectConflictException extends RuntimeException {
    public ProjectConflictException(String message) {
        super(message);
    }
}
