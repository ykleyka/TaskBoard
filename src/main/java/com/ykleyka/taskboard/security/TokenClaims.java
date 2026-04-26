package com.ykleyka.taskboard.security;

import java.time.Instant;

public record TokenClaims(Long userId, Instant expiresAt) {
}
