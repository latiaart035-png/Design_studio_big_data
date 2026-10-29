package com.designstudio.api;

import com.designstudio.application.service.UserService;
import com.designstudio.domain.entity.User;
import com.designstudio.domain.repository.UserRepository;
import com.designstudio.infrastructure.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://localhost:5501", "http://127.0.0.1:5500", "http://127.0.0.1:5501"}, allowCredentials = "true")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            System.out.println("=== getCurrentUser called ===");

            if (userDetails == null) {
                System.out.println("UserDetails is null");
                return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
            }

            User user = userDetails.getUser();
            System.out.println("User: " + (user != null ? user.getEmail() : "null"));

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Пользователь не найден"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail() != null ? user.getEmail() : "");
            response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            response.put("role", user.getRole() != null ? user.getRole().name() : "USER");
            response.put("status", user.getStatus() != null ? user.getStatus().name() : "NEW");
            response.put("discount", user.getDiscount() != null ? user.getDiscount() : 0.0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getCurrentUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    // ==================== ИНФОРМАЦИЯ О СКИДКЕ ====================

    @GetMapping("/discount")
    public ResponseEntity<?> getDiscountInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            UserService.DiscountInfo info = userService.getDiscountInfo(userDetails.getUser().getId());

            if (info == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", info.getStatus());
            response.put("discount", info.getDiscount());
            response.put("discountPercent", info.getDiscountPercent());
            response.put("quarterSpent", info.getQuarterSpent());
            response.put("quarterStart", info.getQuarterStart());
            response.put("quarterEnd", info.getQuarterEnd());
            response.put("premiumThreshold", info.getPremiumThreshold());
            response.put("vipThreshold", info.getVipThreshold());
            response.put("remainingForPremium", info.getRemainingForPremium());
            response.put("remainingForVip", info.getRemainingForVip());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getDiscountInfo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    // ==================== СМЕНА ПАРОЛЯ ====================

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Не авторизован"));
        }

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (currentPassword == null || currentPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите текущий пароль"));
        }

        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введите новый пароль"));
        }

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Подтвердите новый пароль"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Новый пароль и подтверждение не совпадают"));
        }

        UserService.PasswordChangeResult result = userService.changePassword(
                userDetails.getUser().getId(),
                currentPassword,
                newPassword
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", result.getMessage()
            ));
        }
    }
}