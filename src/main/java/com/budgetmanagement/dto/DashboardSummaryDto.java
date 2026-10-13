package com.budgetmanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal balance,
        BigDecimal totalSavings,
        BigDecimal monthlyBudgetUtilization,
        BigDecimal savingsRate,
        List<TransactionResponse> recentTransactions,
        List<BudgetResponse> budgetAlerts,
        List<MonthlyReportDto> monthlyTrend,
        List<CategoryReportDto> categoryBreakdown
) {
}
