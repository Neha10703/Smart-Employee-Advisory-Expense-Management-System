package com.smart.app.repository;

import com.smart.app.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findBySplitExpenseIdOrderBySettledAtDesc(Long splitExpenseId);
    List<Settlement> findBySplitShareIdOrderBySettledAtDesc(Long splitShareId);
}