package com.ykleyka.taskboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ykleyka.taskboard.dto.AuthLoginRequest;
import com.ykleyka.taskboard.dto.AuthRegisterRequest;
import com.ykleyka.taskboard.dto.AuthResponse;
import com.ykleyka.taskboard.dto.UserRequest;
import com.ykleyka.taskboard.dto.UserResponse;
import com.ykleyka.taskboard.mapper.UserMapper;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.repository.UserRepository;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.security.GeneratedToken;
import com.ykleyka.taskboard.security.JwtTokenService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private JwtTokenService tokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService service;

    @Test
    void register_createsUserAndReturnsBearerToken() {
        AuthRegisterRequest request =
                new AuthRegisterRequest("jdoe", "john@example.com", "secret", "John", "Doe");
        User mapped = user(null, "jdoe", "john@example.com");
        User saved = user(1L, "jdoe", "john@example.com");
        UserResponse userResponse = userResponse(1L, "jdoe");
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");

        when(userMapper.toEntity(any(UserRequest.class))).thenReturn(mapped);
        when(userService.createUser(mapped)).thenReturn(saved);
        when(tokenService.generate(saved)).thenReturn(new GeneratedToken("jwt-value", expiresAt));
        when(userMapper.toResponse(saved)).thenReturn(userResponse);

        AuthResponse response = service.register(request);

        assertEquals("Bearer", response.tokenType());
        assertEquals("jwt-value", response.accessToken());
        assertEquals(expiresAt, response.expiresAt());
        assertEquals(userResponse, response.user());
        verify(userMapper).toEntity(new UserRequest("jdoe", "john@example.com", "secret", "John", "Doe"));
    }

    @Test
    void login_whenUsernameAndPasswordMatch_returnsBearerToken() {
        AuthLoginRequest request = new AuthLoginRequest("jdoe", "secret");
        User user = user(2L, "jdoe", "john@example.com");
        user.setPasswordHash("encoded");
        UserResponse userResponse = userResponse(2L, "jdoe");
        Instant expiresAt = Instant.parse("2030-01-02T00:00:00Z");

        when(userRepository.findByUsernameIgnoreCase("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(tokenService.generate(user)).thenReturn(new GeneratedToken("login-jwt", expiresAt));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = service.login(request);

        assertEquals("Bearer", response.tokenType());
        assertEquals("login-jwt", response.accessToken());
        assertEquals(expiresAt, response.expiresAt());
        assertEquals(userResponse, response.user());
    }

    @Test
    void login_whenUsernameMissingButEmailMatches_returnsBearerToken() {
        AuthLoginRequest request = new AuthLoginRequest("john@example.com", "secret");
        User user = user(3L, "jdoe", "john@example.com");
        user.setPasswordHash("encoded");
        Instant expiresAt = Instant.parse("2030-01-03T00:00:00Z");

        when(userRepository.findByUsernameIgnoreCase("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(tokenService.generate(user)).thenReturn(new GeneratedToken("email-jwt", expiresAt));
        when(userMapper.toResponse(user)).thenReturn(userResponse(3L, "jdoe"));

        AuthResponse response = service.login(request);

        assertEquals("email-jwt", response.accessToken());
    }

    @Test
    void login_whenLoginMissing_throwsUnauthorized() {
        AuthLoginRequest request = new AuthLoginRequest("missing", "secret");
        when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.login(request));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsUnauthorized() {
        AuthLoginRequest request = new AuthLoginRequest("jdoe", "wrong");
        User user = user(4L, "jdoe", "john@example.com");
        user.setPasswordHash("encoded");

        when(userRepository.findByUsernameIgnoreCase("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.login(request));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void me_whenCurrentUserExists_returnsProfile() {
        User user = user(5L, "current", "current@example.com");
        UserResponse response = userResponse(5L, "current");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse actual =
                service.me(new AuthenticatedUser(5L, "current", "current@example.com", "Current", "User"));

        assertEquals(response, actual);
    }

    @Test
    void me_whenCurrentUserMissing_throwsUnauthorized() {
        AuthenticatedUser principal =
                new AuthenticatedUser(404L, "missing", "missing@example.com", "Missing", "User");
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.me(principal));

        assertEquals(401, exception.getStatusCode().value());
    }

    private User user(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("encoded");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    private UserResponse userResponse(Long id, String username) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new UserResponse(id, username, username + "@example.com", "John", "Doe", now, now);
    }
}
