package com.ykleyka.taskboard.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ykleyka.taskboard.model.User;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final Duration tokenTtl;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${taskboard.security.jwt-secret:change-me-taskboard-dev-secret}")
                    String jwtSecret,
            @Value("${taskboard.security.token-ttl:PT24H}")
                    Duration tokenTtl) {
        this.objectMapper = objectMapper;
        this.secretKey =
                new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.tokenTtl = tokenTtl;
    }

    public GeneratedToken generate(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload =
                encodeJson(Map.of(
                        "sub", String.valueOf(user.getId()),
                        "username", user.getUsername(),
                        "iat", issuedAt.getEpochSecond(),
                        "exp", expiresAt.getEpochSecond()));
        String unsignedToken = header + "." + payload;
        return new GeneratedToken(unsignedToken + "." + sign(unsignedToken), expiresAt);
    }

    public TokenClaims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BadCredentialsException("Malformed bearer token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BadCredentialsException("Invalid bearer token signature");
        }

        JsonNode payload = readPayload(parts[1]);
        long expiresAtEpoch = requiredLong(payload, "exp");
        Instant expiresAt = Instant.ofEpochSecond(expiresAtEpoch);
        if (!expiresAt.isAfter(Instant.now())) {
            throw new BadCredentialsException("Bearer token expired");
        }

        return new TokenClaims(requiredUserId(payload), expiresAt);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize token", exception);
        }
    }

    private JsonNode readPayload(String encodedPayload) {
        try {
            return objectMapper.readTree(DECODER.decode(encodedPayload));
        } catch (IllegalArgumentException | IOException exception) {
            throw new BadCredentialsException("Malformed bearer token payload", exception);
        }
    }

    private Long requiredUserId(JsonNode payload) {
        JsonNode subject = payload.get("sub");
        if (subject == null || subject.asText().isBlank()) {
            throw new BadCredentialsException("Bearer token is missing subject");
        }
        try {
            return Long.parseLong(subject.asText());
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Bearer token subject is invalid", exception);
        }
    }

    private long requiredLong(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new BadCredentialsException("Bearer token is missing " + field);
        }
        return value.asLong();
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign bearer token", exception);
        }
    }
}
