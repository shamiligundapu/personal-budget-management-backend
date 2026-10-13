package com.budgetmanagement.dto;

import com.budgetmanagement.enums.GoalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequest(
        @NotBlank(message = "Goal title is required")
        @Size(max = 120, message = "Title cannot exceed 120 characters")
        String title,

        @NotNull(message = "Target amount is required")
        @DecimalMin(value = "0.01", message = "Target amount must be greater than zero")
        BigDecimal targetAmount,

        @NotNull(message = "Current amount is required")
        @DecimalMin(value = "0.00", message = "Current amount cannot be negative")
        BigDecimal currentAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate,

        GoalStatus status
) {
}
