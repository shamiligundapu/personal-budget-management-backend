package com.budgetmanagement.dto;

import com.budgetmanagement.enums.Role;

import java.time.LocalDateTime;

public record UserDto(Long id, String fullName, String email, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
