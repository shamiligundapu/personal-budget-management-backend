package com.budgetmanagement.service;

import com.budgetmanagement.dto.CategoryRequest;
import com.budgetmanagement.dto.CategoryResponse;
import com.budgetmanagement.entity.Category;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.CategoryType;
import com.budgetmanagement.exception.DuplicateResourceException;
import com.budgetmanagement.exception.ResourceNotFoundException;
import com.budgetmanagement.repository.CategoryRepository;
import com.budgetmanagement.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public List<CategoryResponse> getUserCategories(User user) {
        ensureDefaultCategories(user);
        return categoryRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void ensureDefaultCategories(User user) {
        List<String> defaultExpenseCategories = List.of(
                "Housing",
                "Food",
                "Transport",
                "Utilities",
                "Entertainment",
                "Shopping",
                "Health",
                "Education",
                "Travel",
                "Other"
        );

        List<String> defaultIncomeCategories = List.of(
                "Salary",
                "Freelance",
                "Business",
                "Investment",
                "Gift",
                "Other Income"
        );

        createDefaultsIfMissing(user, defaultExpenseCategories, CategoryType.EXPENSE);
        createDefaultsIfMissing(user, defaultIncomeCategories, CategoryType.INCOME);
    }

    private void createDefaultsIfMissing(User user, List<String> names, CategoryType type) {
        List<String> existingNames = categoryRepository.findByUserIdAndTypeOrderByNameAsc(user.getId(), type).stream()
                .map(Category::getName)
                .toList();

        for (String name : names) {
            String normalized = name.trim();
            if (existingNames.stream().anyMatch(existing -> existing.equalsIgnoreCase(normalized))) {
                continue;
            }
            categoryRepository.save(new Category(normalized, type, "Default category", user));
        }
    }

    @Transactional
    public CategoryResponse createCategory(User user, CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.findByUserIdOrderByNameAsc(user.getId()).stream()
                .anyMatch(category -> category.getName().equalsIgnoreCase(name)
                        && category.getType() == request.type())) {
            throw new DuplicateResourceException("A category with the same name and type already exists");
        }

        Category category = new Category(name, request.type(), request.description(), user);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(User user, Long id, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String name = request.name().trim();
        category.setName(name);
        category.setType(request.type());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(User user, Long id) {
        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (transactionRepository.findByUserIdWithCategory(user.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).stream()
                .anyMatch(t -> t.getCategory().getId().equals(id))) {
            throw new IllegalStateException("Cannot delete a category that is currently used by a transaction");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryByIdForUser(User user, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType(), category.getDescription(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
