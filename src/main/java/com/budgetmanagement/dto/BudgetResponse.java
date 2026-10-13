package com.budgetmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponse(Long id,
                            Long categoryId,
                            String categoryName,
                            BigDecimal monthlyLimit,
                            int month,
                            int year,
                            BigDecimal utilizationPercentage,
                            BigDecimal totalSpent,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
}
