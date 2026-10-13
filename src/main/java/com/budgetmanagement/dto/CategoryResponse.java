package com.budgetmanagement.dto;

import com.budgetmanagement.enums.CategoryType;

import java.time.LocalDateTime;

public record CategoryResponse(Long id, String name, CategoryType type, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
