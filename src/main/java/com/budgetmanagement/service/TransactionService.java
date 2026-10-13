package com.budgetmanagement.service;

import com.budgetmanagement.dto.TransactionRequest;
import com.budgetmanagement.dto.TransactionResponse;
import com.budgetmanagement.entity.Category;
import com.budgetmanagement.entity.Transaction;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.exception.InvalidRequestException;
import com.budgetmanagement.exception.ResourceNotFoundException;
import com.budgetmanagement.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(User user, TransactionType type, Long categoryId,
                                                  LocalDate fromDate, LocalDate toDate, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be non-negative and page size must be between 1 and 100");
        }
        if (fromDate == null) {
            fromDate = LocalDate.now().withDayOfMonth(1);
        }
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate.isAfter(toDate)) {
            throw new InvalidRequestException("Start date must be on or before end date");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return transactionRepository.findByFilters(user.getId(), type, categoryId, fromDate, toDate, pageable).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(User user, Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to access this transaction");
        }
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse createTransaction(User user, TransactionRequest request) {
        Category category = categoryService.getCategoryByIdForUser(user, request.categoryId());
        com.budgetmanagement.enums.TransactionType expectedType = category.getType() == com.budgetmanagement.enums.CategoryType.INCOME
                ? com.budgetmanagement.enums.TransactionType.INCOME
                : com.budgetmanagement.enums.TransactionType.EXPENSE;
        if (request.type() != expectedType) {
            throw new InvalidRequestException("Transaction type does not match the category type");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setPaymentMethod(request.paymentMethod());

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse updateTransaction(User user, Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to update this transaction");
        }

        Category category = categoryService.getCategoryByIdForUser(user, request.categoryId());
        com.budgetmanagement.enums.TransactionType expectedType = category.getType() == com.budgetmanagement.enums.CategoryType.INCOME
                ? com.budgetmanagement.enums.TransactionType.INCOME
                : com.budgetmanagement.enums.TransactionType.EXPENSE;
        if (request.type() != expectedType) {
            throw new InvalidRequestException("Transaction type does not match the category type");
        }

        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setPaymentMethod(request.paymentMethod());
        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(User user, Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to delete this transaction");
        }
        transactionRepository.delete(transaction);
    }

    public BigDecimal getTotalForUserAndType(User user, TransactionType type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserIdAndTypeAndDateBetween(user.getId(), type, start, end);
    }

    private TransactionResponse toResponse(Transaction transaction) {
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
