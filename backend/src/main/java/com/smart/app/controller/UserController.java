package com.smart.app.controller;

import com.smart.app.model.User;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, Object> updates, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (updates.containsKey("name")) user.setName((String) updates.get("name"));
        if (updates.containsKey("phone")) user.setPhone((String) updates.get("phone"));
        if (updates.containsKey("address")) user.setAddress((String) updates.get("address"));
        if (updates.containsKey("occupation")) user.setOccupation((String) updates.get("occupation"));
        if (updates.containsKey("monthlyBudget")) user.setMonthlyBudget(Double.valueOf(updates.get("monthlyBudget").toString()));
        if (updates.containsKey("budgetAlertThreshold")) user.setBudgetAlertThreshold(Double.valueOf(updates.get("budgetAlertThreshold").toString()));
        if (updates.containsKey("emailNotifications")) user.setEmailNotifications((Boolean) updates.get("emailNotifications"));
        if (updates.containsKey("pushNotifications")) user.setPushNotifications((Boolean) updates.get("pushNotifications"));
        if (updates.containsKey("budgetAlerts")) user.setBudgetAlerts((Boolean) updates.get("budgetAlerts"));
        if (updates.containsKey("twoFactorEnabled")) user.setTwoFactorEnabled((Boolean) updates.get("twoFactorEnabled"));

        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Current password is incorrect");
            return ResponseEntity.badRequest().body(response);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            if (file.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "No file selected");
                return ResponseEntity.badRequest().body(response);
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get("uploads/profiles/");
            Files.createDirectories(uploadPath);
            
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            user.setProfilePhoto("/uploads/profiles/" + fileName);
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("photoUrl", user.getProfilePhoto());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}