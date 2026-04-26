package com.ykleyka.taskboard.controller;

import com.ykleyka.taskboard.dto.DashboardResponse;
import com.ykleyka.taskboard.security.AuthenticatedUser;
import com.ykleyka.taskboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated workspace data for the authenticated user")
public class DashboardController {
    private final DashboardService service;

    @Operation(summary = "Get dashboard summary", description = "Returns scoped dashboard metrics.")
    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.getDashboard(currentUser.id());
    }
}
