package com.smart.app.service;

import com.smart.app.model.Document;
import com.smart.app.model.User;
import com.smart.app.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class GovernmentDocumentService {

    private final DocumentRepository documentRepository;

    // Simulate government document databases
    private final Map<String, List<Map<String, Object>>> governmentDatabases = new HashMap<String, List<Map<String, Object>>>() {{
        put("AADHAAR", List.of(
            Map.of("docId", "AADHAAR_001", "name", "Aadhaar Card", "issuer", "UIDAI", "status", "Active"),
            Map.of("docId", "AADHAAR_002", "name", "Aadhaar Enrollment Receipt", "issuer", "UIDAI", "status", "Active")
        ));
        put("PAN", List.of(
            Map.of("docId", "PAN_001", "name", "PAN Card", "issuer", "Income Tax Department", "status", "Active"),
            Map.of("docId", "PAN_002", "name", "PAN Application Receipt", "issuer", "NSDL", "status", "Active")
        ));
        put("PASSPORT", List.of(
            Map.of("docId", "PASSPORT_001", "name", "Passport", "issuer", "Ministry of External Affairs", "status", "Active"),
            Map.of("docId", "PASSPORT_002", "name", "Passport Application Receipt", "issuer", "PSK", "status", "Active")
        ));
        put("DRIVING_LICENSE", List.of(
            Map.of("docId", "DL_001", "name", "Driving License", "issuer", "RTO", "status", "Active"),
            Map.of("docId", "DL_002", "name", "Learner's License", "issuer", "RTO", "status", "Active")
        ));
        put("VOTER_ID", List.of(
            Map.of("docId", "VOTER_001", "name", "Voter ID Card", "issuer", "Election Commission", "status", "Active")
        ));
        put("EDUCATION", List.of(
            Map.of("docId", "EDU_001", "name", "10th Marksheet", "issuer", "CBSE", "status", "Active"),
            Map.of("docId", "EDU_002", "name", "12th Marksheet", "issuer", "CBSE", "status", "Active"),
            Map.of("docId", "EDU_003", "name", "Degree Certificate", "issuer", "University", "status", "Active")
        ));
        put("MEDICAL", List.of(
            Map.of("docId", "MED_001", "name", "Vaccination Certificate", "issuer", "Ministry of Health", "status", "Active"),
            Map.of("docId", "MED_002", "name", "Medical Fitness Certificate", "issuer", "Government Hospital", "status", "Active")
        ));
    }};

    public List<Map<String, Object>> searchGovernmentDocuments(String category) {
        return governmentDatabases.getOrDefault(category.toUpperCase(), new ArrayList<>());
    }

    public List<String> getAvailableCategories() {
        return new ArrayList<>(governmentDatabases.keySet());
    }

    public Document fetchGovernmentDocument(User user, String docId, String category) {
        // Simulate fetching from government database
        List<Map<String, Object>> categoryDocs = governmentDatabases.get(category.toUpperCase());
        
        if (categoryDocs != null) {
            for (Map<String, Object> doc : categoryDocs) {
                if (doc.get("docId").equals(docId)) {
                    // Create document entry
                    Document document = Document.builder()
                            .user(user)
                            .documentName((String) doc.get("name"))
                            .documentType(category)
                            .filePath("government/documents/" + docId + ".pdf")
                            .fileSize(1024000L) // 1MB default
                            .uploadDate(LocalDateTime.now())
                            .build();
                    
                    return documentRepository.save(document);
                }
            }
        }
        
        throw new RuntimeException("Document not found in government database");
    }

    public boolean isDocumentAlreadyFetched(User user, String docId) {
        return documentRepository.findByUserOrderByUploadDateDesc(user)
                .stream()
                .anyMatch(doc -> doc.getFilePath().contains(docId));
    }
}