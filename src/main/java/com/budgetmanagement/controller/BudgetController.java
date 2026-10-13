package com.budgetmanagement.controller;

import com.budgetmanagement.dto.BudgetRequest;
import com.budgetmanagement.dto.BudgetResponse;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.BudgetService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {
    private final BudgetService budgetService;
    private final UserService userService;

    public BudgetController(BudgetService budgetService, UserService userService) {
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @GetMapping
    public List<BudgetResponse> getBudgets() {
        User user = userService.getCurrentUser();
        return budgetService.getUserBudgets(user);
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(budgetService.createBudget(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(budgetService.updateBudget(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        budgetService.deleteBudget(user, id);
        return ResponseEntity.noContent().build();
    }
}
