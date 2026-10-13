package com.budgetmanagement.service;

import com.budgetmanagement.dto.BudgetResponse;
import com.budgetmanagement.dto.CategoryReportDto;
import com.budgetmanagement.dto.DashboardSummaryDto;
import com.budgetmanagement.dto.MonthlyReportDto;
import com.budgetmanagement.dto.TransactionResponse;
import com.budgetmanagement.entity.Transaction;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.repository.SavingsGoalRepository;
import com.budgetmanagement.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BudgetService budgetService;

    public DashboardService(TransactionRepository transactionRepository,
                           SavingsGoalRepository savingsGoalRepository,
                           BudgetService budgetService) {
        this.transactionRepository = transactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.budgetService = budgetService;
    }

    public DashboardSummaryDto getSummary(User user, int month, int year) {
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.INCOME, start, end);
        BigDecimal totalExpenses = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal balance = totalIncome.subtract(totalExpenses);
        BigDecimal totalSavings = savingsGoalRepository.findByUserIdOrderByTargetDateAsc(user.getId()).stream()
                .map(goal -> goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? totalIncome.subtract(totalExpenses).multiply(new BigDecimal("100")).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<TransactionResponse> recentTransactions = transactionRepository.findByUserIdWithCategory(user.getId(), PageRequest.of(0, 5)).stream()
                .map(this::toTransactionResponse)
                .toList();

        List<BudgetResponse> budgetAlerts = budgetService.findBudgetAlerts(user, month, year);
        List<MonthlyReportDto> monthlyTrend = buildMonthlyTrend(user, year);
        List<CategoryReportDto> categoryBreakdown = buildCategoryBreakdown(user, month, year);
        BigDecimal utilization = budgetService.getMonthlyBudgetUtilization(user, month, year);

        return new DashboardSummaryDto(totalIncome, totalExpenses, balance, totalSavings, utilization, savingsRate, recentTransactions, budgetAlerts, monthlyTrend, categoryBreakdown);
    }

    private List<MonthlyReportDto> buildMonthlyTrend(User user, int year) {
        List<MonthlyReportDto> trend = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            BigDecimal income = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.INCOME, start, end);
            BigDecimal expense = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, start, end);
            trend.add(new MonthlyReportDto(ym.getMonth().name(), income, expense, income.subtract(expense)));
        }
        return trend;
    }

    private List<CategoryReportDto> buildCategoryBreakdown(User user, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Transaction> txns = transactionRepository.findByUserIdAndDateRangeWithCategory(user.getId(), start, end, PageRequest.of(0, Integer.MAX_VALUE));

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Transaction txn : txns) {
            if (txn.getType() != TransactionType.EXPENSE) {
                continue;
            }
            totals.merge(txn.getCategory().getName(), txn.getAmount(), BigDecimal::add);
        }

        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new CategoryReportDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getPaymentMethod(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
