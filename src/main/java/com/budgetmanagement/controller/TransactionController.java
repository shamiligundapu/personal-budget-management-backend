package com.budgetmanagement.controller;

import com.budgetmanagement.dto.TransactionRequest;
import com.budgetmanagement.dto.TransactionResponse;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.TransactionType;
import com.budgetmanagement.exception.InvalidRequestException;
import com.budgetmanagement.service.TransactionService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.getCurrentUser();
        TransactionType transactionType = type == null || type.isBlank() ? null : TransactionType.valueOf(type.toUpperCase());
        LocalDate fromDate = from == null || from.isBlank() ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(from);
        LocalDate toDate = to == null || to.isBlank() ? LocalDate.now() : LocalDate.parse(to);
        return transactionService.getTransactions(user, transactionType, categoryId, fromDate, toDate, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(transactionService.getTransactionById(user, id));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(transactionService.createTransaction(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(transactionService.updateTransaction(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        transactionService.deleteTransaction(user, id);
        return ResponseEntity.noContent().build();
    }
}
