package com.ykleyka.taskboard.controller;
import com.ykleyka.taskboard.dto.UserPatchRequest;
import com.ykleyka.taskboard.dto.UserRequest;
import com.ykleyka.taskboard.dto.UserResponse;
import com.ykleyka.taskboard.mapper.UserMapper;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.service.UserService;
import com.ykleyka.taskboard.validation.OnCreate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations for managing users")
public class UserController {
    private final UserMapper mapper;
    private final UserService service;

    @Operation(summary = "List users", description = "Returns a paginated list of users.")
    @GetMapping
    public List<UserResponse> getUsers(
            @ParameterObject @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getUsers(pageable).stream().map(mapper::toResponse).toList();
    }

    @Operation(summary = "Get user by id", description = "Returns a single user by identifier.")
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable @Positive Long id) {
        return mapper.toResponse(service.getUserById(id));
    }

    @Operation(summary = "Create user", description = "Creates a new user.")
    @PostMapping
    public UserResponse createUser(
            @Validated({Default.class, OnCreate.class}) @RequestBody UserRequest request) {
        return mapper.toResponse(service.createUser(mapper.toEntity(request)));
    }

    @Operation(summary = "Replace user", description = "Fully updates an existing user.")
    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable @Positive Long id, @Valid @RequestBody UserRequest request) {
        User user = mapper.toEntity(request);
        return mapper.toResponse(service.updateUser(id, user));
    }

    @Operation(summary = "Patch user", description = "Partially updates an existing user.")
    @PatchMapping("/{id}")
    public UserResponse patchUser(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UserPatchRequest request) {
        return mapper.toResponse(service.patchUser(id, request));
    }

    @Operation(summary = "Delete user", description = "Deletes a user and returns the removed entity.")
    @DeleteMapping("/{id}")
    public UserResponse deleteUser(@PathVariable @Positive Long id) {
        return mapper.toResponse(service.deleteUser(id));
    }
}
