package com.smart.app.repository;

import com.smart.app.model.BankAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BankAgentRepository extends JpaRepository<BankAgent, Long> {
    List<BankAgent> findByIsActiveTrue();
    List<BankAgent> findByBankNameContainingIgnoreCase(String bankName);
}
