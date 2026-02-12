package com.smart.app.repository;

import com.smart.app.model.PendingSplitParticipant;
import com.smart.app.model.SplitExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingSplitParticipantRepository extends JpaRepository<PendingSplitParticipant, Long> {
    List<PendingSplitParticipant> findByEmail(String email);
    List<PendingSplitParticipant> findByEmailAndNotifiedFalse(String email);
    List<PendingSplitParticipant> findBySplitExpense(SplitExpense splitExpense);
    void deleteByEmail(String email);
}