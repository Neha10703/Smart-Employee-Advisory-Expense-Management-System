package com.smart.app.controller;

import com.smart.app.model.User;
import com.smart.app.repository.UserRepository;
import com.smart.app.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DebugController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @GetMapping("/admin-user")
    public ResponseEntity<Map<String, Object>> checkAdminUser() {
        Map<String, Object> response = new HashMap<>();
        
        User adminUser = userRepository.findByEmail("admin@smart.com").orElse(null);
        
        if (adminUser != null) {
            response.put("exists", true);
            response.put("id", adminUser.getId());
            response.put("name", adminUser.getName());
            response.put("email", adminUser.getEmail());
            response.put("role", adminUser.getRole().name());
            response.put("enabled", adminUser.isEnabled());
        } else {
            response.put("exists", false);
            response.put("message", "Admin user not found");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-documents")
    public ResponseEntity<Map<String, Object>> testDocuments() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long totalDocs = documentRepository.count();
            response.put("totalDocuments", totalDocs);
            response.put("message", "Documents API accessible");
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}