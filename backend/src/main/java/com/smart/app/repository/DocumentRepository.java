package com.smart.app.repository;

import com.smart.app.model.Document;
import com.smart.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
    List<Document> findByUserOrderByUploadDateDesc(User user);
}
