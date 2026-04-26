package com.ykleyka.taskboard.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserService userService;

    public AuthResponse register(AuthRegisterRequest request) {
        UserRequest userRequest =
                new UserRequest(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.firstName(),
                        request.lastName());
        User user = userService.createUser(userMapper.toEntity(userRequest));
        return toAuthResponse(user);
    }

    public AuthResponse login(AuthLoginRequest request) {
        User user = findByLogin(request.login());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return toAuthResponse(user);
    }

    public UserResponse me(AuthenticatedUser currentUser) {
        User user = userRepository
                .findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return userMapper.toResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        GeneratedToken token = tokenService.generate(user);
        return new AuthResponse(
                "Bearer",
                token.value(),
                token.expiresAt(),
                userMapper.toResponse(user));
    }

    private User findByLogin(String login) {
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .orElseThrow(this::invalidCredentials);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username/email or password");
    }
}
