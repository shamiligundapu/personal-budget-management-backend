package com.budgetmanagement.service;

import com.budgetmanagement.dto.SavingsGoalRequest;
import com.budgetmanagement.dto.SavingsGoalResponse;
import com.budgetmanagement.entity.SavingsGoal;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.GoalStatus;
import com.budgetmanagement.exception.InvalidRequestException;
import com.budgetmanagement.exception.ResourceNotFoundException;
import com.budgetmanagement.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository savingsGoalRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoals(User user) {
        return savingsGoalRepository.findByUserIdOrderByTargetDateAsc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavingsGoalResponse createGoal(User user, SavingsGoalRequest request) {
        validateGoal(request);
        SavingsGoal goal = new SavingsGoal(user, request.title().trim(), request.targetAmount(), request.currentAmount(), request.targetDate(), request.status() == null ? GoalStatus.ACTIVE : request.status());
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public SavingsGoalResponse updateGoal(User user, Long id, SavingsGoalRequest request) {
        validateGoal(request);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));

        goal.setTitle(request.title().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());
        goal.setStatus(request.status() == null ? GoalStatus.ACTIVE : request.status());
        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(User user, Long id) {
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found"));
        savingsGoalRepository.delete(goal);
    }

    private void validateGoal(SavingsGoalRequest request) {
        if (request.targetAmount() == null || request.targetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Target amount must be greater than zero");
        }
        if (request.currentAmount() == null || request.currentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Current amount cannot be negative");
        }
        if (request.currentAmount().compareTo(request.targetAmount()) > 0) {
            throw new InvalidRequestException("Current amount cannot exceed target amount");
        }
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal percentage = BigDecimal.ZERO;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = goal.getCurrentAmount().multiply(new BigDecimal("100")).divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        }
        return new SavingsGoalResponse(goal.getId(), goal.getTitle(), goal.getTargetAmount(), goal.getCurrentAmount(), goal.getTargetDate(), goal.getStatus(), percentage, goal.getCreatedAt(), goal.getUpdatedAt());
    }
}
