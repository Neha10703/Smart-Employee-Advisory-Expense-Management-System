package com.smart.app.controller;

import com.smart.app.model.Document;
import com.smart.app.model.User;
import com.smart.app.repository.DocumentRepository;
import com.smart.app.repository.UserRepository;
import com.smart.app.service.GovernmentDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DigitalLockerController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final GovernmentDocumentService governmentDocumentService;
    private final String uploadDir = "uploads/documents/";

    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        
        List<Document> documents = documentRepository.findByUserOrderByUploadDateDesc(user);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/government/categories")
    public ResponseEntity<List<String>> getGovernmentCategories() {
        return ResponseEntity.ok(governmentDocumentService.getAvailableCategories());
    }

    @GetMapping("/government/search/{category}")
    public ResponseEntity<List<Map<String, Object>>> searchGovernmentDocuments(@PathVariable String category) {
        return ResponseEntity.ok(governmentDocumentService.searchGovernmentDocuments(category));
    }

    @PostMapping("/government/fetch")
    public ResponseEntity<Document> fetchGovernmentDocument(
            @RequestParam("docId") String docId,
            @RequestParam("category") String category,
            Authentication authentication) {
        
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            // Check if document already fetched
            if (governmentDocumentService.isDocumentAlreadyFetched(user, docId)) {
                return ResponseEntity.badRequest().build();
            }

            Document document = governmentDocumentService.fetchGovernmentDocument(user, docId, category);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentName") String documentName,
            @RequestParam("documentType") String documentType,
            Authentication authentication) {
        
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, file.getBytes());

            // Save document metadata
            Document document = Document.builder()
                    .user(user)
                    .documentName(documentName)
                    .documentType(documentType)
                    .filePath(uploadDir + uniqueFilename)
                    .fileSize(file.getSize())
                    .uploadDate(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(documentRepository.save(document));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            Document document = documentRepository.findById(id).orElse(null);
            if (document == null || !document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + document.getDocumentName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            Document document = documentRepository.findById(id).orElse(null);
            if (document == null || !document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }

            // Delete file from filesystem
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete from database
            documentRepository.delete(document);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}