package com.smart.app.controller;

import com.smart.app.model.FAQ;
import com.smart.app.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final FAQRepository faqRepository;

    @GetMapping("/faqs")
    public ResponseEntity<Map<String, List<FAQ>>> getFAQs() {
        List<FAQ> faqs = faqRepository.findByIsActiveTrueOrderByCategoryAscQuestionAsc();
        
        Map<String, List<FAQ>> groupedFAQs = new HashMap<>();
        for (FAQ faq : faqs) {
            groupedFAQs.computeIfAbsent(faq.getCategory(), k -> new ArrayList<>()).add(faq);
        }
        
        return ResponseEntity.ok(groupedFAQs);
    }

    @PostMapping("/ai-response")
    public ResponseEntity<Map<String, String>> getAIResponse(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String response = getPredefindResponse(question);
        
        Map<String, String> result = new HashMap<>();
        result.put("question", question);
        result.put("response", response);
        
        return ResponseEntity.ok(result);
    }

    private String getPredefindResponse(String question) {
        Map<String, String> responses = new HashMap<>();
        
        // Split Expense Issues
        responses.put("How does equal split work?", 
            "Equal split divides the total amount equally among all participants including you. For example, ₹300 among 3 people = ₹100 each.");
        responses.put("Why is someone pending payment?", 
            "A participant shows 'pending' when they haven't paid their share yet. As creator, you can mark them as paid once they pay you directly.");
        responses.put("How to settle split expense?", 
            "Participants can click 'Pay Now' to pay via UPI, or pay you directly and you can mark them as paid in the split details.");
        responses.put("Can I edit split after creation?", 
            "Currently, splits cannot be edited after creation. You can delete and recreate if needed.");
        
        // Subscription Benefits
        responses.put("What do I get in premium?", 
            "Premium includes unlimited splits, advanced analytics, priority support, and exclusive banking advisory features.");
        responses.put("How to upgrade subscription?", 
            "Go to Settings → Subscription → Choose Premium plan → Complete payment via UPI or card.");
        responses.put("Can I cancel anytime?", 
            "Yes, you can cancel your subscription anytime from Settings. You'll retain premium features until the current billing period ends.");
        responses.put("What payment methods accepted?", 
            "We accept UPI, Credit/Debit Cards, Net Banking, and popular digital wallets like Paytm, PhonePe.");
        
        // Bank Advisory Help
        responses.put("How banking advisory works?", 
            "Our certified financial experts provide personalized advice on loans, investments, and financial planning based on your profile and goals.");
        responses.put("Are advisors certified?", 
            "Yes, all our banking advisors are certified financial professionals with relevant experience and proper credentials.");
        responses.put("What does consultation cost?", 
            "Basic consultation is free for Premium users. Detailed advisory sessions start from ₹500 per session.");
        responses.put("How to book advisory session?", 
            "Go to Banking → Advisory → Select advisor → Choose time slot → Confirm booking. You'll receive confirmation via email.");
        
        // Payment Related Issues
        responses.put("Why payment failed?", 
            "Payment can fail due to insufficient balance, network issues, or bank server problems. Please try again or use a different payment method.");
        responses.put("How to get refund?", 
            "Failed payments are automatically refunded within 3-5 business days. For other refunds, contact support with transaction details.");
        responses.put("Which UPI apps supported?", 
            "All major UPI apps are supported: PhonePe, Paytm, GPay, BHIM, Amazon Pay, and any UPI-enabled banking app.");
        responses.put("Is payment data secure?", 
            "Yes, we use bank-grade encryption and don't store your payment details. All transactions are processed through secure payment gateways.");
        
        return responses.getOrDefault(question, "I'm here to help! Please select one of the available options for instant assistance.");
    }
}