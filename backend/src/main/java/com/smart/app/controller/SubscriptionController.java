package com.smart.app.controller;

import com.smart.app.config.RazorpayConfig;
import com.smart.app.model.Subscription;
import com.smart.app.model.User;
import com.smart.app.repository.SubscriptionRepository;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RazorpayConfig razorpayConfig;

    @GetMapping
    public ResponseEntity<Subscription> getCurrentSubscription(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createFreeSubscription(user));

        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<Map<String, Object>> upgradeSubscription(
            @RequestBody Map<String, String> request, 
            Authentication authentication) {
        
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) return ResponseEntity.notFound().build();

            String planStr = request.get("plan");
            String billingCycleStr = request.get("billingCycle");
            
            Subscription.SubscriptionPlan plan = Subscription.SubscriptionPlan.valueOf(planStr);
            Subscription.BillingCycle billingCycle = Subscription.BillingCycle.valueOf(billingCycleStr);
            
            double amount = calculateAmount(plan, billingCycle);
            
            // Create Razorpay client
            com.razorpay.RazorpayClient razorpay = new com.razorpay.RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());
            
            // Create order
            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", (int)(amount * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sub_" + user.getId() + "_" + System.currentTimeMillis());
            
            com.razorpay.Order order = razorpay.orders.create(orderRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("key", razorpayConfig.getKeyId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to create order: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<Map<String, String>> verifyPayment(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        // verify payment with Razorpay
        String paymentId = request.get("paymentId");
        String orderId = request.get("orderId");
        String signature = request.get("signature");
        String plan = request.get("plan");
        String billingCycle = request.get("billingCycle");

        // Update subscription
        Subscription subscription = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> createFreeSubscription(user));

        subscription.setPlan(Subscription.SubscriptionPlan.valueOf(plan));
        subscription.setBillingCycle(Subscription.BillingCycle.valueOf(billingCycle));
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        
        if (billingCycle.equals("MONTHLY")) {
            subscription.setEndDate(LocalDateTime.now().plusMonths(1));
            subscription.setNextBillingDate(LocalDateTime.now().plusMonths(1));
        } else {
            subscription.setEndDate(LocalDateTime.now().plusYears(1));
            subscription.setNextBillingDate(LocalDateTime.now().plusYears(1));
        }
        
        subscription.setAmount(calculateAmount(Subscription.SubscriptionPlan.valueOf(plan), 
                                            Subscription.BillingCycle.valueOf(billingCycle)));
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        // Update user subscription plan
        user.setSubscriptionPlan(plan);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription upgraded successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        Subscription subscription = subscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null) return ResponseEntity.notFound().build();

        // Mark as cancelled but keep active until end date
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription cancelled. Access continues until " + subscription.getEndDate());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Map<String, String>> reactivateSubscription(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        Subscription subscription = subscriptionRepository.findByUserId(user.getId()).orElse(null);
        if (subscription == null || subscription.getStatus() != Subscription.SubscriptionStatus.CANCELLED) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "No cancelled subscription found");
            return ResponseEntity.badRequest().body(response);
        }

        // Reactivate subscription
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription reactivated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getSubscriptionPlans() {
        Map<String, Object> plans = new HashMap<>();
        
        Map<String, Object> free = new HashMap<>();
        free.put("name", "Free");
        free.put("monthlyPrice", 0);
        free.put("yearlyPrice", 0);
        free.put("features", new String[]{"Basic expense tracking", "Monthly budget setting", "Limited split expenses", "Basic dashboard"});
        
        Map<String, Object> silver = new HashMap<>();
        silver.put("name", "Silver");
        silver.put("monthlyPrice", 299);
        silver.put("yearlyPrice", 2999);
        silver.put("features", new String[]{"Unlimited expenses", "Unlimited split expenses", "Advanced analytics", "Budget alerts", "Banking advisory"});
        
        Map<String, Object> gold = new HashMap<>();
        gold.put("name", "Gold");
        gold.put("monthlyPrice", 599);
        gold.put("yearlyPrice", 5999);
        gold.put("features", new String[]{"All Silver features", "Premium banking advisory", "Investment advisory", "Advanced insights", "Priority support"});
        
        plans.put("FREE", free);
        plans.put("SILVER", silver);
        plans.put("GOLD", gold);
        
        return ResponseEntity.ok(plans);
    }

    private Subscription createFreeSubscription(User user) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(Subscription.SubscriptionPlan.FREE);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        return subscriptionRepository.save(subscription);
    }

    private double calculateAmount(Subscription.SubscriptionPlan plan, Subscription.BillingCycle billingCycle) {
        double monthlyPrice = switch (plan) {
            case SILVER -> 299.0;
            case GOLD -> 599.0;
            default -> 0.0;
        };
        
        return billingCycle == Subscription.BillingCycle.YEARLY ? monthlyPrice * 10 : monthlyPrice;
    }
}