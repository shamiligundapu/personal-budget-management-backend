package com.budgetmanagement.dto;

import java.math.BigDecimal;

public record MonthlyReportDto(String month, BigDecimal income, BigDecimal expense, BigDecimal balance) {
}
