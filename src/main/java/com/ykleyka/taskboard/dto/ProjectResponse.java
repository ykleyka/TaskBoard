package com.ykleyka.taskboard.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id, String name, String description, Instant createdAt, Instant updatedAt) {

}
