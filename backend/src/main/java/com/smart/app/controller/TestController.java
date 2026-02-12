package com.smart.app.controller;

import com.smart.app.repository.DocumentRepository;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TestController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long userCount = userRepository.count();
            response.put("userCount", userCount);
            response.put("status", "Database connection OK");
            
            try {
                long docCount = documentRepository.count();
                response.put("documentCount", docCount);
                response.put("documentsTable", "OK");
            } catch (Exception e) {
                response.put("documentsTable", "ERROR: " + e.getMessage());
                response.put("documentCount", 0);
            }
            
        } catch (Exception e) {
            response.put("status", "Database connection failed");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}