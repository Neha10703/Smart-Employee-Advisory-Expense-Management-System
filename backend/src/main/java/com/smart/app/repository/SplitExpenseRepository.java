package com.smart.app.repository;

import com.smart.app.model.SplitExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitExpenseRepository extends JpaRepository<SplitExpense, Long> {
    
    @Query("SELECT se FROM SplitExpense se WHERE se.creator.id = :userId OR se.id IN " +
           "(SELECT sp.splitExpense.id FROM SplitParticipant sp WHERE sp.user.id = :userId)")
    List<SplitExpense> findUserSplitExpenses(@Param("userId") Long userId);
    
    List<SplitExpense> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
}