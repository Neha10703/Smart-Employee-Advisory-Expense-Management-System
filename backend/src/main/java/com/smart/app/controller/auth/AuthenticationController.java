package com.smart.app.controller.auth;

import com.smart.app.service.AuthenticationService;
import com.smart.app.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/register/bank")
    public ResponseEntity<AuthenticationResponse> registerBank(
            @RequestBody BankRegisterRequest request) {
        try {
            return ResponseEntity.ok(service.registerBank(request));
        } catch (Exception e) {
            throw new RuntimeException("Bank registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {
        var response = service.authenticate(request);
        
        // Create session
        var user = service.getUserByEmail(request.getEmail());
        String sessionId = sessionService.createSession(
            user, 
            getClientIpAddress(httpRequest), 
            httpRequest.getHeader("User-Agent")
        );
        
        // Add session ID to response
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(response.getToken())
                .sessionId(sessionId)
                .build());
    }

    @PostMapping("/forgot-password/email")
    public ResponseEntity<Map<String, String>> getSecurityQuestion(
            @RequestBody ForgotPasswordRequest request) {
        try {
            String question = service.getSecurityQuestion(request.getEmail());
            Map<String, String> response = new HashMap<>();
            response.put("securityQuestion", question);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Email not found or no security question set");
        }
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<Map<String, String>> verifySecurityAnswer(
            @RequestBody SecurityAnswerRequest request) {
        try {
            System.out.println("Verifying security answer for email: " + request.getEmail());
            System.out.println("Provided answer: " + request.getSecurityAnswer());
            
            boolean isValid = service.verifySecurityAnswer(request.getEmail(), request.getSecurityAnswer());
            if (isValid) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Security answer verified");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Incorrect security answer");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            System.out.println("Error verifying security answer: " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        try {
            service.resetPassword(request.getEmail(), request.getNewPassword());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId != null) {
                sessionService.invalidateSession(sessionId);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Logout failed");
        }
    }
    
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, String>> checkEmail(
            @RequestParam String email) {
        try {
            boolean exists = service.emailExists(email);
            if (exists) {
                return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
            }
            return ResponseEntity.ok(Map.of("message", "Email available"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error checking email"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
