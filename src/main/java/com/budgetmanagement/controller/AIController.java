package com.budgetmanagement.controller;

import com.budgetmanagement.dto.AiChatRequest;
import com.budgetmanagement.dto.AiChatResponse;
import com.budgetmanagement.entity.User;
import com.budgetmanagement.service.AIService;
import com.budgetmanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
public class AIController {
    private final AIService aiService;
    private final UserService userService;

    public AIController(AIService aiService, UserService userService) {
        this.aiService = aiService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(aiService.chat(user, request));
    }
}
