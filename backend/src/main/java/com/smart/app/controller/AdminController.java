package com.smart.app.controller;

import com.smart.app.model.*;
import com.smart.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SplitExpenseRepository splitExpenseRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private BankAgentRepository bankAgentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        long totalUsers = userRepository.countByRole(Role.USER);
        long totalBanks = userRepository.countByRole(Role.BANK);
        long totalExpenses = expenseRepository.count();
        long totalSplitExpenses = splitExpenseRepository.count();
        long totalSubscriptions = subscriptionRepository.count();
        long activeSubscriptions = subscriptionRepository.findAll().stream()
            .filter(sub -> sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
            .count();
        long blockedUsers = userRepository.findAll().stream()
            .filter(user -> !user.isEnabled())
            .count();
        
        // Calculate monthly revenue
        double monthlyRevenue = subscriptionRepository.findAll().stream()
            .filter(sub -> sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE)
            .mapToDouble(sub -> sub.getAmount() != null ? sub.getAmount() : 0.0)
            .sum();
        
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("totalBanks", totalBanks);
        dashboard.put("totalExpenses", totalExpenses);
        dashboard.put("totalSplitExpenses", totalSplitExpenses);
        dashboard.put("totalSubscriptions", totalSubscriptions);
        dashboard.put("activeSubscriptions", activeSubscriptions);
        dashboard.put("blockedUsers", blockedUsers);
        dashboard.put("monthlyRevenue", monthlyRevenue);
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findByRole(Role.USER);
        List<Map<String, Object>> userDetails = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("enabled", user.isEnabled());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("subscriptionPlan", user.getSubscriptionPlan());
            
            // Get subscription details
            subscriptionRepository.findByUserId(user.getId()).ifPresent(sub -> {
                userMap.put("subscriptionStatus", sub.getStatus());
                userMap.put("subscriptionEndDate", sub.getEndDate());
            });
            
            return userMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/banks")
    public ResponseEntity<List<Map<String, Object>>> getAllBanks() {
        List<User> banks = userRepository.findByRole(Role.BANK);
        List<Map<String, Object>> bankDetails = banks.stream().map(bank -> {
            Map<String, Object> bankMap = new HashMap<>();
            bankMap.put("id", bank.getId());
            bankMap.put("name", bank.getName());
            bankMap.put("bankName", bank.getBankName());
            bankMap.put("ifscCode", bank.getIfscCode());
            bankMap.put("email", bank.getEmail());
            bankMap.put("phone", bank.getPhone());
            bankMap.put("enabled", bank.isEnabled());
            bankMap.put("createdAt", bank.getCreatedAt());
            
            // Count bank agents
            long agentCount = bankAgentRepository.findByBankNameContainingIgnoreCase(bank.getBankName()).size();
            bankMap.put("agentCount", agentCount);
            
            return bankMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(bankDetails);
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getAllExpenses() {
        List<Expense> expenses = expenseRepository.findAll();
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/split-expenses")
    public ResponseEntity<List<SplitExpense>> getAllSplitExpenses() {
        List<SplitExpense> splitExpenses = splitExpenseRepository.findAll();
        return ResponseEntity.ok(splitExpenses);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        
        return ResponseEntity.ok(user.isEnabled() ? "User activated" : "User deactivated");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "User not found");
                return ResponseEntity.status(404).body(response);
            }
            
            // Prevent deleting admin users
            if (user.getRole() == Role.ADMIN) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Cannot delete admin users");
                return ResponseEntity.status(400).body(response);
            }
            
            // Delete associated data first
            try {
                expenseRepository.deleteAll(expenseRepository.findByUserIdOrderByExpenseDateDesc(user.getId()));
            } catch (Exception e) {
                System.out.println("Error deleting expenses: " + e.getMessage());
            }
            
            try {
                subscriptionRepository.findByUserId(user.getId()).ifPresent(sub -> subscriptionRepository.delete(sub));
            } catch (Exception e) {
                System.out.println("Error deleting subscription: " + e.getMessage());
            }
            
            // Now delete the user
            userRepository.deleteById(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("Error in deleteUser: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        if (!expenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        expenseRepository.deleteById(id);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @DeleteMapping("/split-expenses/{id}")
    public ResponseEntity<String> deleteSplitExpense(@PathVariable Long id) {
        if (!splitExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        splitExpenseRepository.deleteById(id);
        return ResponseEntity.ok("Split expense deleted successfully");
    }

    // Subscription Management
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<Map<String, Object>> subscriptionDetails = subscriptions.stream().map(sub -> {
            Map<String, Object> subMap = new HashMap<>();
            subMap.put("id", sub.getId());
            subMap.put("userId", sub.getUser().getId());
            subMap.put("userName", sub.getUser().getName());
            subMap.put("userEmail", sub.getUser().getEmail());
            subMap.put("plan", sub.getPlan());
            subMap.put("status", sub.getStatus());
            subMap.put("billingCycle", sub.getBillingCycle());
            subMap.put("amount", sub.getAmount());
            subMap.put("startDate", sub.getStartDate());
            subMap.put("endDate", sub.getEndDate());
            subMap.put("nextBillingDate", sub.getNextBillingDate());
            subMap.put("razorpaySubscriptionId", sub.getRazorpaySubscriptionId());
            return subMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(subscriptionDetails);
    }

    @PutMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<String> cancelSubscription(@PathVariable Long id) {
        Subscription subscription = subscriptionRepository.findById(id).orElse(null);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        
        return ResponseEntity.ok("Subscription cancelled successfully");
    }

    // Bank Management
    @PutMapping("/banks/{id}/status")
    public ResponseEntity<String> toggleBankStatus(@PathVariable Long id) {
        User bank = userRepository.findById(id).orElse(null);
        if (bank == null || bank.getRole() != Role.BANK) {
            return ResponseEntity.notFound().build();
        }
        
        bank.setEnabled(!bank.isEnabled());
        userRepository.save(bank);
        
        return ResponseEntity.ok(bank.isEnabled() ? "Bank activated" : "Bank deactivated");
    }

    @DeleteMapping("/banks/{id}")
    public ResponseEntity<String> deleteBank(@PathVariable Long id) {
        User bank = userRepository.findById(id).orElse(null);
        if (bank == null || bank.getRole() != Role.BANK) {
            return ResponseEntity.notFound().build();
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok("Bank deleted successfully");
    }

    // Search functionality
    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(@RequestParam String query) {
        List<User> users = userRepository.findByRole(Role.USER).stream()
            .filter(user -> user.getName().toLowerCase().contains(query.toLowerCase()) ||
                           user.getEmail().toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());
        
        List<Map<String, Object>> userDetails = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("enabled", user.isEnabled());
            userMap.put("subscriptionPlan", user.getSubscriptionPlan());
            return userMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(userDetails);
    }

    // System health endpoint
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            long userCount = userRepository.count();
            health.put("database", "healthy");
            health.put("totalRecords", userCount);
        } catch (Exception e) {
            health.put("database", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        health.put("timestamp", LocalDateTime.now());
        health.put("status", "operational");
        
        return ResponseEntity.ok(health);
    }

    // Payment Monitoring
    @GetMapping("/payments")
    public ResponseEntity<List<Map<String, Object>>> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        List<Map<String, Object>> paymentDetails = payments.stream().map(payment -> {
            Map<String, Object> paymentMap = new HashMap<>();
            paymentMap.put("id", payment.getId());
            paymentMap.put("userId", payment.getUser().getId());
            paymentMap.put("userName", payment.getUser().getName());
            paymentMap.put("userEmail", payment.getUser().getEmail());
            paymentMap.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            paymentMap.put("razorpayOrderId", payment.getRazorpayOrderId());
            paymentMap.put("status", payment.getStatus());
            paymentMap.put("method", payment.getMethod());
            paymentMap.put("amount", payment.getAmount());
            paymentMap.put("currency", payment.getCurrency());
            paymentMap.put("description", payment.getDescription());
            paymentMap.put("createdAt", payment.getCreatedAt());
            
            if (payment.getSubscription() != null) {
                paymentMap.put("subscriptionPlan", payment.getSubscription().getPlan());
            }
            
            return paymentMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(paymentDetails);
    }

    @GetMapping("/payments/failed")
    public ResponseEntity<List<Map<String, Object>>> getFailedPayments() {
        List<Payment> failedPayments = paymentRepository.findByStatus(Payment.PaymentStatus.FAILED);
        List<Map<String, Object>> paymentDetails = failedPayments.stream().map(payment -> {
            Map<String, Object> paymentMap = new HashMap<>();
            paymentMap.put("id", payment.getId());
            paymentMap.put("userId", payment.getUser().getId());
            paymentMap.put("userName", payment.getUser().getName());
            paymentMap.put("userEmail", payment.getUser().getEmail());
            paymentMap.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            paymentMap.put("amount", payment.getAmount());
            paymentMap.put("createdAt", payment.getCreatedAt());
            return paymentMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(paymentDetails);
    }
}