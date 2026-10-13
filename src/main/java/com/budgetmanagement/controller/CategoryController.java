package com.budgetmanagement.controller;

import com.budgetmanagement.dto.CategoryRequest;
import com.budgetmanagement.dto.CategoryResponse;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.CategoryService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {
    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        User user = userService.getCurrentUser();
        return categoryService.getUserCategories(user);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(categoryService.createCategory(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(categoryService.updateCategory(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        categoryService.deleteCategory(user, id);
        return ResponseEntity.noContent().build();
    }
}
