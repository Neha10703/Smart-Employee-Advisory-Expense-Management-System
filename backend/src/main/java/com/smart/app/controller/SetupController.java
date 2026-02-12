package com.smart.app.controller;

import com.smart.app.model.User;
import com.smart.app.repository.UserRepository;
import com.smart.app.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SetupController {

    private final UserRepository userRepository;
    private final DocumentService documentService;

    @PostMapping("/sample-documents")
    public ResponseEntity<Map<String, String>> createSampleDocuments(Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "User not found");
                return ResponseEntity.notFound().build();
            }

            documentService.createSampleDocuments(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Sample documents created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}