package com.budgetmanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Monthly limit is required")
        @DecimalMin(value = "0.00", message = "Monthly limit cannot be negative")
        BigDecimal monthlyLimit,

        @NotNull(message = "Month is required")
        Integer month,

        @NotNull(message = "Year is required")
        Integer year
) {
}
