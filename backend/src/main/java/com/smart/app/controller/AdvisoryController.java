package com.smart.app.controller;

import com.smart.app.model.Advisory;
import com.smart.app.model.BankAgent;
import com.smart.app.repository.AdvisoryRepository;
import com.smart.app.repository.BankAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/advisory")
@RequiredArgsConstructor
public class AdvisoryController {

    private final AdvisoryRepository advisoryRepository;
    private final BankAgentRepository bankAgentRepository;

    @GetMapping("/suggestions")
    public ResponseEntity<List<Advisory>> getAllSuggestions() {
        return ResponseEntity.ok(advisoryRepository.findAll());
    }

    @PostMapping("/suggestions")
    public ResponseEntity<Advisory> addSuggestion(@RequestBody Advisory advisory) {
        return ResponseEntity.ok(advisoryRepository.save(advisory));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<BankAgent>> getAllAgents() {
        return ResponseEntity.ok(bankAgentRepository.findAll());
    }

    @GetMapping("/agents/{bankName}")
    public ResponseEntity<List<BankAgent>> getAgentsByBank(@PathVariable String bankName) {
        return ResponseEntity.ok(bankAgentRepository.findByBankNameContainingIgnoreCase(bankName));
    }

    @PostMapping("/agents")
    public ResponseEntity<BankAgent> addAgent(@RequestBody BankAgent agent) {
        return ResponseEntity.ok(bankAgentRepository.save(agent));
    }
}
