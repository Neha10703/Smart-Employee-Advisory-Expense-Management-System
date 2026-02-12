package com.smart.app.controller;

import com.smart.app.model.BankAgent;
import com.smart.app.repository.BankAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-agents")
@RequiredArgsConstructor
public class BankAgentController {

    private final BankAgentRepository bankAgentRepository;

    @GetMapping
    public ResponseEntity<List<BankAgent>> getAllBankAgents() {
        return ResponseEntity.ok(bankAgentRepository.findByIsActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAgent> getBankAgent(@PathVariable Long id) {
        return bankAgentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}