package com.budgetmanagement.service;

import com.budgetmanagement.dto.BudgetRequest;
import com.budgetmanagement.dto.BudgetResponse;
import com.budgetmanagement.entity.Budget;
import com.budgetmanagement.entity.Category;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.exception.DuplicateResourceException;
import com.budgetmanagement.exception.InvalidRequestException;
import com.budgetmanagement.exception.ResourceNotFoundException;
import com.budgetmanagement.repository.BudgetRepository;
import com.budgetmanagement.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    @Value("${budget.alert.threshold-percent:80}")
    private int thresholdPercent;

    public BudgetService(BudgetRepository budgetRepository, TransactionRepository transactionRepository, CategoryService categoryService) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getUserBudgets(User user) {
        return budgetRepository.findByUserIdOrderByYearDescMonthDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(User user, BudgetRequest request) {
        validateRequest(request);

        Category category = categoryService.getCategoryByIdForUser(user, request.categoryId());
        validateExpenseCategory(category);
        if (budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(user.getId(), category.getId(), request.month(), request.year()).isPresent()) {
            throw new DuplicateResourceException("A budget for this category and period already exists");
        }

        Budget budget = new Budget(user, category, request.monthlyLimit(), request.month(), request.year());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse updateBudget(User user, Long id, BudgetRequest request) {
        validateRequest(request);
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to update this budget");
        }

        Category category = categoryService.getCategoryByIdForUser(user, request.categoryId());
        validateExpenseCategory(category);
        if (budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(user.getId(), category.getId(), request.month(), request.year())
                .filter(existing -> !existing.getId().equals(id))
                .isPresent()) {
            throw new DuplicateResourceException("A budget for this category and period already exists");
        }
        budget.setCategory(category);
        budget.setMonthlyLimit(request.monthlyLimit());
        budget.setMonth(request.month());
        budget.setYear(request.year());
        return toResponse(budgetRepository.save(budget));
    }

    private void validateRequest(BudgetRequest request) {
        if (request.month() < 1 || request.month() > 12) {
            throw new InvalidRequestException("Month must be between 1 and 12");
        }
        if (request.monthlyLimit() == null || request.monthlyLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Monthly limit cannot be negative");
        }
    }

    private void validateExpenseCategory(Category category) {
        if (category.getType() != com.budgetmanagement.enums.CategoryType.EXPENSE) {
            throw new InvalidRequestException("Budgets can only be assigned to expense categories");
        }
    }

    @Transactional
    public void deleteBudget(User user, Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to delete this budget");
        }
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyBudgetUtilization(User user, int month, int year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        BigDecimal totalLimit = budgets.stream().map(Budget::getMonthlyLimit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        BigDecimal expenseTotal = transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), TransactionType.EXPENSE, start, end);
        return expenseTotal.multiply(new BigDecimal("100")).divide(totalLimit, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findBudgetAlerts(User user, int month, int year) {
        List<BudgetResponse> alerts = new ArrayList<>();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        for (Budget budget : budgets) {
            BudgetResponse response = toResponse(budget);
            if (response.utilizationPercentage().compareTo(BigDecimal.valueOf(thresholdPercent)) >= 0) {
                alerts.add(response);
            }
        }
        alerts.sort(Comparator.comparing(BudgetResponse::year).reversed().thenComparing(BudgetResponse::month).reversed());
        return alerts;
    }

    private BudgetResponse toResponse(Budget budget) {
        BigDecimal totalSpent = transactionRepository.sumByUserIdAndCategoryIdAndType(
                budget.getUser().getId(), budget.getCategory().getId(), TransactionType.EXPENSE);

        BigDecimal utilization = BigDecimal.ZERO;
        if (budget.getMonthlyLimit() != null && budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
            utilization = totalSpent.multiply(new BigDecimal("100")).divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);
        } else if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
            utilization = new BigDecimal("100");
        }

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getMonthlyLimit(),
                budget.getMonth(),
                budget.getYear(),
                utilization,
                totalSpent,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
