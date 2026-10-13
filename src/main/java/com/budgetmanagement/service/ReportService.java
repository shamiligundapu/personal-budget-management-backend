package com.budgetmanagement.service;

import com.budgetmanagement.dto.CategoryReportDto;
import com.budgetmanagement.dto.MonthlyReportDto;
import com.budgetmanagement.entity.Transaction;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<MonthlyReportDto> getMonthlyReport(User user, int year) {
        List<MonthlyReportDto> reports = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            BigDecimal income = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.INCOME, start, end);
            BigDecimal expense = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, start, end);
            reports.add(new MonthlyReportDto(ym.getMonth().name(), income, expense, income.subtract(expense)));
        }
        return reports;
    }

    public List<CategoryReportDto> getCategoryReport(User user, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateRangeWithCategory(user.getId(), start, end, PageRequest.of(0, Integer.MAX_VALUE));
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                totals.merge(transaction.getCategory().getName(), transaction.getAmount(), BigDecimal::add);
            }
        }

        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new CategoryReportDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
