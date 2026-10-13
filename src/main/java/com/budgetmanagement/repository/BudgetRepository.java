package com.budgetmanagement.repository;

import com.budgetmanagement.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdOrderByYearDescMonthDesc(Long userId);
    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);
    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    @Query("SELECT COALESCE(SUM(b.monthlyLimit), 0) FROM Budget b WHERE b.user.id = :userId AND b.month = :month AND b.year = :year")
    BigDecimal sumMonthlyLimitByUserAndMonthYear(@Param("userId") Long userId,
                                                @Param("month") int month,
                                                @Param("year") int year);
}
