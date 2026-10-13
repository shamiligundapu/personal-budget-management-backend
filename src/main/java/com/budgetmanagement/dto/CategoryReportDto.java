package com.budgetmanagement.dto;

import java.math.BigDecimal;

public record CategoryReportDto(String categoryName, BigDecimal totalAmount) {
}
