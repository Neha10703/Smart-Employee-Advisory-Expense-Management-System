package com.smart.app.repository;

import com.smart.app.model.SplitParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitParticipantRepository extends JpaRepository<SplitParticipant, Long> {
    List<SplitParticipant> findBySplitExpenseId(Long splitExpenseId);
    List<SplitParticipant> findByUserId(Long userId);
}