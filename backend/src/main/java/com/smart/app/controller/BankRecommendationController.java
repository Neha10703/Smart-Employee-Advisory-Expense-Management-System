package com.smart.app.controller;

import com.smart.app.model.BankAgent;
import com.smart.app.repository.BankAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/bank-recommendations")
@RequiredArgsConstructor
public class BankRecommendationController {

    private final BankAgentRepository bankAgentRepository;

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@RequestBody Map<String, Object> request) {
        Double salary = Double.valueOf(request.get("salary").toString());
        String accountType = request.get("accountType").toString();
        Boolean zeroBalance = Boolean.valueOf(request.get("zeroBalance").toString());

        List<Map<String, Object>> recommendations = new ArrayList<>();

        // HDFC Bank
        if (salary >= 25000) {
            Map<String, Object> hdfc = new HashMap<>();
            hdfc.put("bankName", "HDFC Bank");
            hdfc.put("accountType", "Salary Account");
            hdfc.put("minBalance", zeroBalance ? 0 : 10000);
            hdfc.put("interestRate", "3.5% p.a.");
            hdfc.put("facilities", Arrays.asList("UPI", "Net Banking", "Mobile Banking", "ATM"));
            hdfc.put("documents", Arrays.asList("Salary Certificate", "Aadhar", "PAN"));
            hdfc.put("agent", getRandomAgent());
            recommendations.add(hdfc);
        }

        // SBI
        Map<String, Object> sbi = new HashMap<>();
        sbi.put("bankName", "State Bank of India");
        sbi.put("accountType", accountType.equals("Salary") ? "Salary Account" : "Savings Account");
        sbi.put("minBalance", zeroBalance ? 0 : 3000);
        sbi.put("interestRate", "2.70% p.a.");
        sbi.put("facilities", Arrays.asList("UPI", "Net Banking", "ATM"));
        sbi.put("documents", Arrays.asList("Salary Slip", "Aadhar", "PAN"));
        sbi.put("agent", getRandomAgent());
        recommendations.add(sbi);

        // ICICI Bank
        if (salary >= 20000) {
            Map<String, Object> icici = new HashMap<>();
            icici.put("bankName", "ICICI Bank");
            icici.put("accountType", "Salary Account");
            icici.put("minBalance", zeroBalance ? 0 : 5000);
            icici.put("interestRate", "3.25% p.a.");
            icici.put("facilities", Arrays.asList("UPI", "Net Banking", "Mobile Banking", "ATM"));
            icici.put("documents", Arrays.asList("Salary Certificate", "Aadhar", "PAN"));
            icici.put("agent", getRandomAgent());
            recommendations.add(icici);
        }

        return ResponseEntity.ok(recommendations);
    }

    private Map<String, Object> getRandomAgent() {
        List<BankAgent> agents = bankAgentRepository.findAll();
        if (agents.isEmpty()) {
            // Create default agent if none exist
            Map<String, Object> defaultAgent = new HashMap<>();
            defaultAgent.put("name", "Banking Advisor");
            defaultAgent.put("email", "advisor@bank.com");
            defaultAgent.put("contactNumber", "+91-1800-123-456");
            defaultAgent.put("bankName", "Customer Service");
            return defaultAgent;
        }
        BankAgent agent = agents.get(new Random().nextInt(agents.size()));
        Map<String, Object> agentInfo = new HashMap<>();
        agentInfo.put("name", agent.getName());
        agentInfo.put("email", agent.getEmail());
        agentInfo.put("contactNumber", agent.getContactNumber());
        agentInfo.put("bankName", agent.getBankName());
        return agentInfo;
    }
}