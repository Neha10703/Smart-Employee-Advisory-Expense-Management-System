package com.smart.app.service;

import com.smart.app.model.Document;
import com.smart.app.model.User;
import com.smart.app.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public void createSampleDocuments(User user) {
        try {
            // Check if user already has documents
            List<Document> existingDocs = documentRepository.findByUserOrderByUploadDateDesc(user);
            if (!existingDocs.isEmpty()) {
                return; // User already has documents
            }

            // Create sample documents for new users
            Document aadhaar = Document.builder()
                    .user(user)
                    .documentName("Aadhaar Card.pdf")
                    .documentType("Identity")
                    .filePath("uploads/documents/sample-aadhaar.pdf")
                    .fileSize(2457600L)
                    .uploadDate(LocalDateTime.now())
                    .build();

            Document pan = Document.builder()
                    .user(user)
                    .documentName("PAN Card.jpg")
                    .documentType("Identity")
                    .filePath("uploads/documents/sample-pan.jpg")
                    .fileSize(1228800L)
                    .uploadDate(LocalDateTime.now())
                    .build();

            Document statement = Document.builder()
                    .user(user)
                    .documentName("Bank Statement.pdf")
                    .documentType("Banking")
                    .filePath("uploads/documents/sample-statement.pdf")
                    .fileSize(5242880L)
                    .uploadDate(LocalDateTime.now())
                    .build();

            documentRepository.save(aadhaar);
            documentRepository.save(pan);
            documentRepository.save(statement);
        } catch (Exception e) {
            System.err.println("Error creating sample documents: " + e.getMessage());
        }
    }
}