package com.budgetmanagement.controller;

import com.budgetmanagement.dto.CategoryReportDto;
import com.budgetmanagement.dto.MonthlyReportDto;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.ReportService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/monthly")
    public List<MonthlyReportDto> getMonthlyReport(@RequestParam(required = false) Integer year) {
        User user = userService.getCurrentUser();
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        return reportService.getMonthlyReport(user, targetYear);
    }

    @GetMapping("/categories")
    public List<CategoryReportDto> getCategoryReport(@RequestParam(required = false) Integer month,
                                                   @RequestParam(required = false) Integer year) {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now();
        int targetMonth = month == null ? today.getMonthValue() : month;
        int targetYear = year == null ? today.getYear() : year;
        return reportService.getCategoryReport(user, targetMonth, targetYear);
    }
}
