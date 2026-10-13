package com.budgetmanagement.dto;

import com.budgetmanagement.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 80, message = "Category name cannot exceed 80 characters")
        String name,

        @NotNull(message = "Category type is required")
        CategoryType type,

        @Size(max = 200, message = "Description cannot exceed 200 characters")
        String description
) {
}
