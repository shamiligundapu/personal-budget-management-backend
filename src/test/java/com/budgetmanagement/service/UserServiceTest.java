package com.budgetmanagement.service;

import com.budgetmanagement.dto.AuthRequest;
import com.budgetmanagement.dto.AuthResponse;
import com.budgetmanagement.dto.LoginRequest;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.Role;
import com.budgetmanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryService categoryService;

    @Test
    void registerShouldCreateUserAndReturnToken() {
        AuthRequest request = new AuthRequest("Alice Admin", "alice@example.com", "Password123");

        AuthResponse response = userService.register(request);

        assertNotNull(response.token());
        assertNotNull(response.user());
        assertEquals("alice@example.com", response.user().email());
        assertTrue(userRepository.findByEmail("alice@example.com").isPresent());
    }

    @Test
    void loginShouldAuthenticateRegisteredUser() {
        userService.register(new AuthRequest("Bob User", "bob@example.com", "Password123"));

        AuthResponse response = userService.login(new LoginRequest("bob@example.com", "Password123"));

        assertNotNull(response.token());
        assertEquals("bob@example.com", response.user().email());
    }

    @Test
    void passwordShouldBeHashedBeforePersistence() {
        userService.register(new AuthRequest("Carol User", "carol@example.com", "Password123"));

        User user = userRepository.findByEmail("carol@example.com").orElseThrow();
        assertNotEquals("Password123", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    void categoryRequestShouldSeedCategoriesForExistingUser() {
        User existingUser = userRepository.save(new User("Legacy User", "legacy@example.com", "password-hash", Role.USER));

        assertEquals(16, categoryService.getUserCategories(existingUser).size());
    }
}
