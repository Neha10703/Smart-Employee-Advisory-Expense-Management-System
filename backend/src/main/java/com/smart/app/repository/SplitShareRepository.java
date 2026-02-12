package com.smart.app.repository;

import com.smart.app.model.SplitShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitShareRepository extends JpaRepository<SplitShare, Long> {
    List<SplitShare> findBySplitExpenseId(Long splitExpenseId);
    List<SplitShare> findByUserId(Long userId);
}