package com.budgetmanagement.repository;

import com.budgetmanagement.entity.Transaction;
import com.budgetmanagement.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByUserIdAndTypeAndDateBetween(@Param("userId") Long userId,
                                               @Param("type") TransactionType type,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByUserIdAndCategoryIdAndTypeAndDateBetween(@Param("userId") Long userId,
                                                            @Param("categoryId") Long categoryId,
                                                            @Param("type") TransactionType type,
                                                            @Param("start") LocalDate start,
                                                            @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.category.id = :categoryId AND t.type = :type")
    BigDecimal sumByUserIdAndCategoryIdAndType(@Param("userId") Long userId,
                                               @Param("categoryId") Long categoryId,
                                               @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category c WHERE t.user.id = :userId ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByUserIdWithCategory(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category c WHERE t.user.id = :userId AND t.transactionDate BETWEEN :start AND :end ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByUserIdAndDateRangeWithCategory(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end, Pageable pageable);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.category c WHERE t.user.id = :userId AND (:type IS NULL OR t.type = :type) AND (:categoryId IS NULL OR c.id = :categoryId) AND t.transactionDate BETWEEN :start AND :end ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByFilters(@Param("userId") Long userId,
                                   @Param("type") TransactionType type,
                                   @Param("categoryId") Long categoryId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end,
                                   Pageable pageable);
}
