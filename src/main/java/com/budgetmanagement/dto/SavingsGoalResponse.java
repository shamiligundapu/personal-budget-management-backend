package com.budgetmanagement.dto;

import com.budgetmanagement.enums.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavingsGoalResponse(Long id,
                                 String title,
                                 BigDecimal targetAmount,
                                 BigDecimal currentAmount,
                                 LocalDate targetDate,
                                 GoalStatus status,
                                 BigDecimal progressPercentage,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
}
