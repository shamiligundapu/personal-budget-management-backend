package com.budgetmanagement.service;

import com.budgetmanagement.dto.AuthRequest;
import com.budgetmanagement.dto.BudgetRequest;
import com.budgetmanagement.dto.BudgetResponse;
import com.budgetmanagement.dto.CategoryResponse;
import com.budgetmanagement.dto.TransactionRequest;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class BudgetServiceTest {
    @Autowired
    private BudgetService budgetService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void budgetUsageIncludesAllExpensesForItsCategoryRegardlessOfMonth() {
        String email = "budget-" + System.nanoTime() + "@example.com";
        userService.register(new AuthRequest("Budget Test", email, "Password123"));
        User user = userRepository.findByEmail(email).orElseThrow();
        CategoryResponse food = getExpenseCategory(user, "Food");
        CategoryResponse health = getExpenseCategory(user, "Health");
        LocalDate today = LocalDate.now();
        LocalDate budgetDate = today.minusMonths(1);

        budgetService.createBudget(user, new BudgetRequest(
                food.id(), new BigDecimal("100.00"), budgetDate.getMonthValue(), budgetDate.getYear()));
        transactionService.createTransaction(user, new TransactionRequest(
                food.id(), TransactionType.EXPENSE, new BigDecimal("25.00"),
                "Budget usage test", today, "Cash"));
        transactionService.createTransaction(user, new TransactionRequest(
                health.id(), TransactionType.EXPENSE, new BigDecimal("40.00"),
                "Different category test", today, "Cash"));

        BudgetResponse budget = budgetService.getUserBudgets(user).get(0);

        assertEquals(new BigDecimal("25.00"), budget.totalSpent());
        assertEquals(new BigDecimal("25.00"), budget.utilizationPercentage());
    }

    @Test
    void budgetAlertsIncludeSpendingAtEightyPercentAndOverBudgetOnly() {
        String email = "budget-alert-" + System.nanoTime() + "@example.com";
        userService.register(new AuthRequest("Budget Alert Test", email, "Password123"));
        User user = userRepository.findByEmail(email).orElseThrow();
        LocalDate today = LocalDate.now();

        CategoryResponse food = getExpenseCategory(user, "Food");
        CategoryResponse shopping = getExpenseCategory(user, "Shopping");
        CategoryResponse housing = getExpenseCategory(user, "Housing");

        for (CategoryResponse category : List.of(food, shopping, housing)) {
            budgetService.createBudget(user, new BudgetRequest(
                    category.id(), new BigDecimal("100.00"), today.getMonthValue(), today.getYear()));
        }
        transactionService.createTransaction(user, new TransactionRequest(
                food.id(), TransactionType.EXPENSE, new BigDecimal("79.99"),
                "Below alert threshold", today, "Cash"));
        transactionService.createTransaction(user, new TransactionRequest(
                shopping.id(), TransactionType.EXPENSE, new BigDecimal("80.00"),
                "At alert threshold", today, "Cash"));
        transactionService.createTransaction(user, new TransactionRequest(
                housing.id(), TransactionType.EXPENSE, new BigDecimal("125.00"),
                "Over budget", today, "Cash"));

        List<BudgetResponse> alerts = budgetService.findBudgetAlerts(user, today.getMonthValue(), today.getYear());

        assertEquals(List.of("Housing", "Shopping"),
                alerts.stream().map(budget -> budget.categoryName()).sorted().toList());
    }

    private CategoryResponse getExpenseCategory(User user, String name) {
        return categoryService.getUserCategories(user).stream()
                .filter(category -> category.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
