package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.UserPatchRequest;
import com.ykleyka.taskboard.dto.UserRequest;
import com.ykleyka.taskboard.dto.UserResponse;
import com.ykleyka.taskboard.mapper.UserMapper;
import com.ykleyka.taskboard.model.User;
import com.ykleyka.taskboard.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserMapper mapper;
    private final UserService service;

    @GetMapping
    public List<UserResponse> getUsers(
            @PageableDefault(page = 0, size = 20, sort = "id") Pageable pageable) {
        return service.getUsers(pageable).map(mapper::toResponse).getContent();
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return mapper.toResponse(service.getUserById(id));
    }

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return mapper.toResponse(service.createUser(mapper.toEntity(request)));
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        User user = mapper.toEntity(request);
        return mapper.toResponse(service.updateUser(id, user));
    }

    @PatchMapping("/{id}")
    public UserResponse patchUser(@PathVariable Long id, @Valid @RequestBody UserPatchRequest request) {
        return mapper.toResponse(service.patchUser(id, request));
    }

    @DeleteMapping("/{id}")
    public UserResponse deleteUser(@PathVariable Long id) {
        return mapper.toResponse(service.deleteUser(id));
    }
}
