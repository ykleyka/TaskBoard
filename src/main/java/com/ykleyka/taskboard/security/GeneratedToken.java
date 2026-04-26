package com.ykleyka.taskboard.security;

import java.time.Instant;

public record GeneratedToken(String value, Instant expiresAt) {
}
