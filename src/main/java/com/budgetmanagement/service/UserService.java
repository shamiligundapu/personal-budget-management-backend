package com.budgetmanagement.service;

import com.budgetmanagement.dto.AuthRequest;
import com.budgetmanagement.dto.AuthResponse;
import com.budgetmanagement.dto.LoginRequest;
import com.budgetmanagement.dto.PasswordChangeRequest;
import com.budgetmanagement.dto.ProfileUpdateRequest;
import com.budgetmanagement.dto.UserDto;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.enums.Role;
import com.budgetmanagement.exception.DuplicateResourceException;
import com.budgetmanagement.exception.InvalidRequestException;
import com.budgetmanagement.exception.ResourceNotFoundException;
import com.budgetmanagement.repository.UserRepository;
import com.budgetmanagement.security.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String email = request.email().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        categoryService.ensureDefaultCategories(savedUser);
        String token = jwtService.generateToken(savedUser.getEmail());
        return new AuthResponse(token, toDto(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidRequestException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, toDto(user));
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new InvalidRequestException("Authentication required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        return toDto(getCurrentUser());
    }

    @Transactional
    public UserDto updateProfile(ProfileUpdateRequest request) {
        User user = getCurrentUser();
        String email = request.email().trim();
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A user with this email already exists");
                });
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
