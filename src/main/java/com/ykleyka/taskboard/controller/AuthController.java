package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.AuthLoginRequest;
import com.ykleyka.taskboard.dto.AuthRegisterRequest;
import com.ykleyka.taskboard.dto.AuthResponse;
import com.ykleyka.taskboard.dto.UserResponse;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and current user operations")
public class AuthController {
    private final AuthService service;

    @Operation(summary = "Register user", description = "Creates a new user and returns an access token.")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return service.register(request);
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns an access token.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return service.login(request);
    }

    @Operation(summary = "Get CSRF token", description = "Returns a CSRF token for unsafe browser requests.")
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName(),
                "token", csrfToken.getToken());
    }

    @Operation(summary = "Get current user", description = "Returns the authenticated user profile.")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.me(currentUser);
    }
}
