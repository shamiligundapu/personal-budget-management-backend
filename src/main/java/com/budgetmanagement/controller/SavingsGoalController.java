package com.budgetmanagement.controller;

import com.budgetmanagement.dto.SavingsGoalRequest;
import com.budgetmanagement.dto.SavingsGoalResponse;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.SavingsGoalService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {
    private final SavingsGoalService savingsGoalService;
    private final UserService userService;

    public SavingsGoalController(SavingsGoalService savingsGoalService, UserService userService) {
        this.savingsGoalService = savingsGoalService;
        this.userService = userService;
    }

    @GetMapping
    public List<SavingsGoalResponse> getGoals() {
        User user = userService.getCurrentUser();
        return savingsGoalService.getGoals(user);
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> createGoal(@Valid @RequestBody SavingsGoalRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(savingsGoalService.createGoal(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> updateGoal(@PathVariable Long id, @Valid @RequestBody SavingsGoalRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(savingsGoalService.updateGoal(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        savingsGoalService.deleteGoal(user, id);
        return ResponseEntity.noContent().build();
    }
}
