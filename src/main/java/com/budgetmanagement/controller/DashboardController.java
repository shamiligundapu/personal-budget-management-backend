package com.budgetmanagement.controller;

import com.budgetmanagement.dto.DashboardSummaryDto;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.DashboardService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryDto getSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now();
        int targetMonth = month == null ? today.getMonthValue() : month;
        int targetYear = year == null ? today.getYear() : year;
        return dashboardService.getSummary(user, targetMonth, targetYear);
    }
}
