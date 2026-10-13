package com.budgetmanagement.repository;

import com.budgetmanagement.entity.Category;
import com.budgetmanagement.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
