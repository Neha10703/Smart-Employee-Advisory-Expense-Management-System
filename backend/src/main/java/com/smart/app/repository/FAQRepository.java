package com.smart.app.repository;

import com.smart.app.model.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FAQRepository extends JpaRepository<FAQ, Long> {
    List<FAQ> findByIsActiveTrueOrderByCategoryAscQuestionAsc();
    List<FAQ> findByCategoryAndIsActiveTrueOrderByQuestionAsc(String category);
}