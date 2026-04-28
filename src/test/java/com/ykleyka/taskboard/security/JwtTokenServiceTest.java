package com.ykleyka.taskboard.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ykleyka.taskboard.model.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class JwtTokenServiceTest {
    private static final String SECRET = "test-secret-for-jwt";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateAndParse_whenTokenIsValid_returnsUserIdAndExpiration() {
        JwtTokenService service = tokenService(Duration.ofHours(2));
        User user = user(15L, "alice");

        GeneratedToken generated = service.generate(user);
        TokenClaims claims = service.parse(generated.value());

        assertEquals(15L, claims.userId());
        assertEquals(generated.expiresAt().getEpochSecond(), claims.expiresAt().getEpochSecond());
        assertTrue(generated.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void parse_whenTokenHasWrongShape_throwsBadCredentials() {
        JwtTokenService service = tokenService(Duration.ofHours(1));

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse("not-a-jwt"));

        assertEquals("Malformed bearer token", exception.getMessage());
    }

    @Test
    void parse_whenSignatureIsTampered_throwsBadCredentials() {
        JwtTokenService service = tokenService(Duration.ofHours(1));
        String token = service.generate(user(16L, "bob")).value();
        String tampered = token.substring(0, token.length() - 2) + "xx";

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse(tampered));

        assertEquals("Invalid bearer token signature", exception.getMessage());
    }

    @Test
    void parse_whenTokenExpired_throwsBadCredentials() {
        JwtTokenService service = tokenService(Duration.ofSeconds(-1));
        String token = service.generate(user(17L, "charlie")).value();

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse(token));

        assertEquals("Bearer token expired", exception.getMessage());
    }

    @Test
    void parse_whenSubjectMissing_throwsBadCredentials() throws Exception {
        JwtTokenService service = tokenService(Duration.ofHours(1));
        String token = signedToken(Map.of("exp", Instant.now().plusSeconds(3600).getEpochSecond()));

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse(token));

        assertEquals("Bearer token is missing subject", exception.getMessage());
    }

    @Test
    void parse_whenSubjectIsNotNumber_throwsBadCredentials() throws Exception {
        JwtTokenService service = tokenService(Duration.ofHours(1));
        String token =
                signedToken(
                        Map.of(
                                "sub", "not-number",
                                "exp", Instant.now().plusSeconds(3600).getEpochSecond()));

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse(token));

        assertEquals("Bearer token subject is invalid", exception.getMessage());
    }

    @Test
    void parse_whenExpirationMissing_throwsBadCredentials() throws Exception {
        JwtTokenService service = tokenService(Duration.ofHours(1));
        String token = signedToken(Map.of("sub", "18"));

        BadCredentialsException exception =
                assertThrows(BadCredentialsException.class, () -> service.parse(token));

        assertEquals("Bearer token is missing exp", exception.getMessage());
    }

    @Test
    void generatedToken_recordStoresValueAndExpiration() {
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
        GeneratedToken token = new GeneratedToken("token-value", expiresAt);

        assertEquals("token-value", token.value());
        assertEquals(expiresAt, token.expiresAt());
    }

    @Test
    void tokenClaims_recordStoresUserIdAndExpiration() {
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
        TokenClaims claims = new TokenClaims(10L, expiresAt);

        assertEquals(10L, claims.userId());
        assertEquals(expiresAt, claims.expiresAt());
    }

    private JwtTokenService tokenService(Duration ttl) {
        return new JwtTokenService(objectMapper, SECRET, ttl);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private String signedToken(Map<String, Object> payload) throws Exception {
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String body = encodeJson(payload);
        String unsignedToken = header + "." + body;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    }
}
