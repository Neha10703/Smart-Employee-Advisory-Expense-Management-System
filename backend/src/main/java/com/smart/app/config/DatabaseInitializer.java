package com.smart.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Initializing database...");
            
            // Check documents table exists and has correct structure
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM documents", Integer.class);
                System.out.println("Documents table exists, checking structure...");
                
                // Check if correct columns exist
                try {
                    jdbcTemplate.queryForObject("SELECT document_name FROM documents LIMIT 1", String.class);
                    System.out.println("Documents table structure is correct.");
                } catch (Exception e) {
                    System.out.println("Documents table structure is incorrect, recreating...");
                    recreateDocumentsTable();
                }
            } catch (Exception e) {
                System.out.println("Documents table doesn't exist, creating...");
                recreateDocumentsTable();
            }
            
            
            addTestBankUser();
            
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
    
    private void recreateDocumentsTable() {
        try {
            // Drop existing table
            jdbcTemplate.execute("DROP TABLE IF EXISTS documents");
            
            // Create documents table
            jdbcTemplate.execute("""
                CREATE TABLE documents (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    document_name VARCHAR(255) NOT NULL,
                    document_type VARCHAR(100) NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    file_size BIGINT,
                    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);
            
            // Insert sample documents
            jdbcTemplate.execute("""
                INSERT INTO documents (user_id, document_name, document_type, file_path, file_size, upload_date) VALUES
                (2, 'Aadhaar Card.pdf', 'Identity', 'uploads/documents/sample-aadhaar.pdf', 2457600, NOW()),
                (2, 'PAN Card.jpg', 'Identity', 'uploads/documents/sample-pan.jpg', 1228800, NOW()),
                (2, 'Bank Statement.pdf', 'Banking', 'uploads/documents/sample-statement.pdf', 5242880, NOW())
            """);
            
            System.out.println("Documents table created successfully with sample data!");
            
        } catch (Exception e) {
            System.err.println("Failed to recreate documents table: " + e.getMessage());
        }
    }
    
    private void addTestBankUser() {
        try {
            // Check if bank user already exists
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'icici@bank.com'", 
                Integer.class
            );
            
            if (count == 0) {
                jdbcTemplate.execute("""
                    INSERT INTO users (name, bank_name, ifsc_code, email, password, role, phone, address, security_question, security_answer, enabled, created_at) 
                    VALUES (
                        'ICICI Bank Manager', 
                        'ICICI Bank', 
                        'ICIC0000001', 
                        'icici@bank.com', 
                        '$2a$10$eImiTXuWVxfM37uY4JANjOhSzm6pFBA2G2qVqXiflUOpBOzQiAuZe', 
                        'BANK', 
                        '9876543210', 
                        '123 Bank Street, Mumbai', 
                        'What is your favorite color?', 
                        '$2a$10$eImiTXuWVxfM37uY4JANjOhSzm6pFBA2G2qVqXiflUOpBOzQiAuZe', 
                        TRUE, 
                        NOW()
                    )
                """);
                System.out.println("Test bank user created: icici@bank.com / admin123");
            } else {
                System.out.println("Test bank user already exists.");
            }
        } catch (Exception e) {
            System.err.println("Failed to create test bank user: " + e.getMessage());
        }
    }
}