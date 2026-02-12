package com.smart.app.controller;

import com.smart.app.model.*;
import com.smart.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/splits")
@RequiredArgsConstructor
public class SplitExpenseController {

    private final SplitExpenseRepository splitExpenseRepository;
    private final SplitParticipantRepository splitParticipantRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PendingSplitParticipantRepository pendingSplitParticipantRepository;

    private User getCurrentUser() {
        try {
            System.out.println("Getting current user...");
            var auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication: " + auth);
            System.out.println("Principal: " + auth.getPrincipal());
            
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            System.out.println("Username: " + userDetails.getUsername());
            
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            System.out.println("Found user: " + user.getEmail());
            return user;
        } catch (Exception e) {
            System.out.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testAuth() {
        try {
            User user = getCurrentUser();
            return ResponseEntity.ok("Authentication working for: " + user.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Auth failed: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSplitExpense(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("Creating split expense with request: " + request);
            User creator = getCurrentUser();
            System.out.println("Creator found: " + creator.getEmail());
            
            SplitExpense splitExpense = SplitExpense.builder()
                    .title(request.get("title").toString())
                    .totalAmount(Double.valueOf(request.get("totalAmount").toString()))
                    .splitType(SplitExpense.SplitType.valueOf(request.get("splitType").toString()))
                    .status(SplitExpense.ExpenseStatus.PENDING)
                    .creator(creator)
                    .createdAt(LocalDateTime.now())
                    .build();

            splitExpense = splitExpenseRepository.save(splitExpense);
            System.out.println("Split expense saved with ID: " + splitExpense.getId());

            // Send notification to creator about split creation
            try {
                Notification creatorNotification = Notification.builder()
                        .user(creator)
                        .title("Split Created")
                        .message("You created a split '" + splitExpense.getTitle() + "' for ₹" + splitExpense.getTotalAmount())
                        .type(Notification.NotificationType.SPLIT_CREATED)
                        .relatedExpenseId(splitExpense.getId())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                notificationRepository.save(creatorNotification);
                System.out.println("Sent creation notification to creator");
            } catch (Exception e) {
                System.out.println("Error sending creator notification: " + e.getMessage());
            }

            List<Map<String, Object>> participantData = (List<Map<String, Object>>) request.get("participants");
            System.out.println("Processing participants: " + participantData);
            
            try {
                createParticipants(splitExpense, participantData);
                System.out.println("Participants processed successfully");
            } catch (Exception e) {
                System.out.println("Error processing participants: " + e.getMessage());
                e.printStackTrace();
                // Don't fail the entire request if participant processing fails
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", splitExpense.getId());
            response.put("message", "Split expense created successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error creating split expense: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/check-emails")
    public ResponseEntity<Map<String, Object>> checkEmails(@RequestBody Map<String, Object> request) {
        List<String> emails = (List<String>) request.get("emails");
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> emailStatus = new ArrayList<>();
        
        for (String email : emails) {
            User user = userRepository.findByEmail(email).orElse(null);
            Map<String, Object> status = new HashMap<>();
            status.put("email", email);
            status.put("registered", user != null);
            if (user != null) {
                status.put("name", user.getName());
            }
            emailStatus.add(status);
        }
        
        result.put("emailStatus", emailStatus);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process-pending")
    @Transactional
    public ResponseEntity<Map<String, Object>> processPendingParticipants() {
        User currentUser = getCurrentUser();
        List<PendingSplitParticipant> pendingParticipants = pendingSplitParticipantRepository.findByEmailAndNotifiedFalse(currentUser.getEmail());
        
        int processedCount = 0;
        for (PendingSplitParticipant pending : pendingParticipants) {
            // Create actual participant record
            SplitParticipant participant = SplitParticipant.builder()
                    .splitExpense(pending.getSplitExpense())
                    .user(currentUser)
                    .shareAmount(pending.getShareAmount())
                    .paymentStatus(SplitParticipant.PaymentStatus.PENDING)
                    .build();
            splitParticipantRepository.save(participant);
            
            // Create notification
            sendInAppNotification(pending.getSplitExpense(), currentUser, pending.getShareAmount());
            
            // Mark as notified
            pending.setNotified(true);
            pendingSplitParticipantRepository.save(pending);
            
            processedCount++;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("processedCount", processedCount);
        response.put("message", processedCount > 0 ? "Processed " + processedCount + " pending splits" : "No pending splits found");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        User currentUser = getCurrentUser();
        List<PendingSplitParticipant> pendingParticipants = pendingSplitParticipantRepository.findByEmailAndNotifiedFalse(currentUser.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("pendingCount", pendingParticipants.size());
        response.put("message", "Found " + pendingParticipants.size() + " pending splits for " + currentUser.getEmail());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-create")
    public ResponseEntity<String> testCreate() {
        try {
            User currentUser = getCurrentUser();
            return ResponseEntity.ok("Ready to create splits for user: " + currentUser.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUserSplitExpenses() {
        User currentUser = getCurrentUser();
        List<SplitExpense> expenses = splitExpenseRepository.findUserSplitExpenses(currentUser.getId());
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (SplitExpense expense : expenses) {
            Map<String, Object> expenseData = new HashMap<>();
            expenseData.put("id", expense.getId());
            expenseData.put("title", expense.getTitle());
            expenseData.put("totalAmount", expense.getTotalAmount());
            expenseData.put("createdAt", expense.getCreatedAt());
            expenseData.put("splitType", expense.getSplitType());
            expenseData.put("status", expense.getStatus());
            expenseData.put("creatorName", expense.getCreator().getName());
            expenseData.put("isCreator", expense.getCreator().getId().equals(currentUser.getId()));
            
            // Get user's share if participant
            List<SplitParticipant> participants = splitParticipantRepository.findBySplitExpenseId(expense.getId());
            for (SplitParticipant participant : participants) {
                if (participant.getUser().getId().equals(currentUser.getId())) {
                    expenseData.put("myShare", participant.getShareAmount());
                    expenseData.put("myPaymentStatus", participant.getPaymentStatus());
                    break;
                }
            }
            
            result.add(expenseData);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSplitExpenseDetails(@PathVariable Long id) {
        SplitExpense expense = splitExpenseRepository.findById(id).orElse(null);
        if (expense == null) return ResponseEntity.notFound().build();

        User currentUser = getCurrentUser();
        
        // Check if user has access
        boolean hasAccess = expense.getCreator().getId().equals(currentUser.getId());
        if (!hasAccess) {
            List<SplitParticipant> participants = splitParticipantRepository.findBySplitExpenseId(id);
            hasAccess = participants.stream().anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));
            
            // Also check pending participants
            if (!hasAccess) {
                List<PendingSplitParticipant> pendingParticipants = pendingSplitParticipantRepository.findBySplitExpense(expense);
                hasAccess = pendingParticipants.stream().anyMatch(p -> p.getEmail().equals(currentUser.getEmail()));
            }
        }
        
        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", expense.getId());
        response.put("title", expense.getTitle());
        response.put("totalAmount", expense.getTotalAmount());
        response.put("createdAt", expense.getCreatedAt());
        response.put("splitType", expense.getSplitType());
        response.put("status", expense.getStatus());
        response.put("creatorName", expense.getCreator().getName());
        response.put("isCreator", expense.getCreator().getId().equals(currentUser.getId()));
        
        // Get registered participants
        List<SplitParticipant> participants = splitParticipantRepository.findBySplitExpenseId(id);
        List<Map<String, Object>> participantList = new ArrayList<>();
        
        for (SplitParticipant participant : participants) {
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("id", participant.getId());
            participantData.put("userName", participant.getUser().getName());
            participantData.put("userEmail", participant.getUser().getEmail());
            participantData.put("shareAmount", participant.getShareAmount());
            participantData.put("paymentStatus", participant.getPaymentStatus());
            participantData.put("paidAt", participant.getPaidAt());
            participantData.put("isCurrentUser", participant.getUser().getId().equals(currentUser.getId()));
            participantData.put("isRegistered", true);
            participantList.add(participantData);
        }
        
        // Get pending participants (unregistered users)
        List<PendingSplitParticipant> pendingParticipants = pendingSplitParticipantRepository.findBySplitExpense(expense);
        System.out.println("Found " + pendingParticipants.size() + " pending participants");
        
        for (PendingSplitParticipant pending : pendingParticipants) {
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("id", "pending_" + pending.getId());
            participantData.put("userName", pending.getEmail().split("@")[0]);
            participantData.put("userEmail", pending.getEmail());
            participantData.put("shareAmount", pending.getShareAmount());
            participantData.put("paymentStatus", "PENDING_REGISTRATION");
            participantData.put("paidAt", null);
            participantData.put("isCurrentUser", pending.getEmail().equals(currentUser.getEmail()));
            participantData.put("isRegistered", false);
            participantList.add(participantData);
            System.out.println("Added pending participant: " + pending.getEmail());
        }
        
        response.put("participants", participantList);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<String> markParticipantAsPaid(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            SplitExpense expense = splitExpenseRepository.findById(id).orElse(null);
            
            if (expense == null) {
                return ResponseEntity.status(404).body("Split not found");
            }
            
            if (!expense.getCreator().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Only creator can mark payments");
            }
            
            Long participantId = Long.valueOf(request.get("participantId").toString());
            SplitParticipant participant = splitParticipantRepository.findById(participantId).orElse(null);
            
            if (participant == null || !participant.getSplitExpense().getId().equals(id)) {
                return ResponseEntity.status(404).body("Participant not found");
            }
            
            participant.setPaymentStatus(SplitParticipant.PaymentStatus.PAID);
            participant.setPaidAt(LocalDateTime.now());
            splitParticipantRepository.save(participant);
            
            // Notify creator about payment
            try {
                Notification paymentNotification = Notification.builder()
                        .user(expense.getCreator())
                        .title("Payment Received")
                        .message(participant.getUser().getName() + " paid ₹" + participant.getShareAmount() + " for '" + expense.getTitle() + "'")
                        .type(Notification.NotificationType.PAYMENT_RECEIVED)
                        .relatedExpenseId(expense.getId())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                notificationRepository.save(paymentNotification);
                System.out.println("Sent payment notification to creator");
            } catch (Exception e) {
                System.out.println("Error sending payment notification: " + e.getMessage());
            }
            
            // Check if all participants have paid
            List<SplitParticipant> allParticipants = splitParticipantRepository.findBySplitExpenseId(id);
            boolean allPaid = allParticipants.stream()
                    .allMatch(p -> p.getPaymentStatus() == SplitParticipant.PaymentStatus.PAID);
                    
            if (allPaid) {
                expense.setStatus(SplitExpense.ExpenseStatus.COMPLETED);
                splitExpenseRepository.save(expense);
            }
            
            return ResponseEntity.ok("Payment marked successfully");
        } catch (Exception e) {
            System.out.println("Error marking payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error marking payment: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> deleteSplitExpense(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            SplitExpense expense = splitExpenseRepository.findById(id).orElse(null);
            
            if (expense == null) {
                return ResponseEntity.status(404).body("Split not found");
            }
            
            if (!expense.getCreator().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Only creator can delete split");
            }
            
            // Delete related records first
            splitParticipantRepository.deleteAll(splitParticipantRepository.findBySplitExpenseId(id));
            pendingSplitParticipantRepository.deleteAll(pendingSplitParticipantRepository.findBySplitExpense(expense));
            
            // Delete the split expense
            splitExpenseRepository.delete(expense);
            
            return ResponseEntity.ok("Split deleted successfully");
        } catch (Exception e) {
            System.out.println("Error deleting split: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting split: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<String> sendEmailInvitation(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            User currentUser = getCurrentUser();
            SplitExpense expense = splitExpenseRepository.findById(id).orElse(null);
            
            if (expense == null || !expense.getCreator().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Only creator can send invitations");
            }
            
            String email = request.get("email").toString();
            
            // Send email invitation (placeholder - integrate with actual email service)
            System.out.println("Sending email invitation to: " + email);
            System.out.println("Subject: You're invited to join a split expense");
            System.out.println("Message: " + currentUser.getName() + " has added you to the split '" + expense.getTitle() + "'. Please register at our app to view and pay your share.");
            
            return ResponseEntity.ok("Invitation sent to " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending invitation: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> initiatePayment(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            
            List<SplitParticipant> participants = splitParticipantRepository.findBySplitExpenseId(id);
            SplitParticipant userParticipant = participants.stream()
                    .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                    .findFirst()
                    .orElse(null);
                    
            if (userParticipant == null) {
                return ResponseEntity.status(403).body(Map.of("error", "You are not a participant in this split"));
            }
            
            if (userParticipant.getPaymentStatus() == SplitParticipant.PaymentStatus.PAID) {
                return ResponseEntity.status(400).body(Map.of("error", "You have already paid for this split"));
            }
            
            SplitExpense expense = userParticipant.getSplitExpense();
            
            // Generate UPI payment URL
            String upiId = "merchant@paytm"; // Replace with actual merchant UPI ID
            String amount = String.valueOf(userParticipant.getShareAmount());
            String note = "Payment for " + expense.getTitle();
            
            String upiUrl = String.format("upi://pay?pa=%s&am=%s&tn=%s&cu=INR", 
                upiId, amount, note.replace(" ", "%20"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("upiUrl", upiUrl);
            response.put("amount", userParticipant.getShareAmount());
            response.put("merchantUpi", upiId);
            response.put("note", note);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error initiating payment: " + e.getMessage()));
        }
    }

    private void createParticipants(SplitExpense splitExpense, List<Map<String, Object>> participantData) {
        try {
            double totalAmount = splitExpense.getTotalAmount();
            int participantCount = participantData.size() + 1; // +1 for creator

            // First create participant record for creator
            double creatorShare = 0;
            if (splitExpense.getSplitType() == SplitExpense.SplitType.EQUAL) {
                creatorShare = totalAmount / participantCount;
            }
            
            SplitParticipant creatorParticipant = SplitParticipant.builder()
                    .splitExpense(splitExpense)
                    .user(splitExpense.getCreator())
                    .shareAmount(creatorShare)
                    .paymentStatus(SplitParticipant.PaymentStatus.PAID) // Creator already paid
                    .paidAt(LocalDateTime.now())
                    .build();
            splitParticipantRepository.save(creatorParticipant);
            System.out.println("Created creator participant with share: " + creatorShare);

            for (Map<String, Object> data : participantData) {
                String email = data.get("email").toString();
                User user = userRepository.findByEmail(email).orElse(null);
                
                double shareAmount = 0;
                if (splitExpense.getSplitType() == SplitExpense.SplitType.EQUAL) {
                    shareAmount = totalAmount / participantCount;
                } else if (splitExpense.getSplitType() == SplitExpense.SplitType.EXACT) {
                    shareAmount = Double.valueOf(data.get("amount").toString());
                }

                if (user != null) {
                    // User is registered - create participant and send notification
                    SplitParticipant participant = SplitParticipant.builder()
                            .splitExpense(splitExpense)
                            .user(user)
                            .shareAmount(shareAmount)
                            .paymentStatus(SplitParticipant.PaymentStatus.PENDING)
                            .build();
                    splitParticipantRepository.save(participant);
                    
                    // Send in-app notification
                    try {
                        sendInAppNotification(splitExpense, user, shareAmount);
                    } catch (Exception e) {
                        System.out.println("Error sending notification: " + e.getMessage());
                    }
                } else {
                    // User not registered - create pending participant record
                    try {
                        PendingSplitParticipant pendingParticipant = PendingSplitParticipant.builder()
                                .splitExpense(splitExpense)
                                .email(email)
                                .shareAmount(shareAmount)
                                .notified(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                        pendingSplitParticipantRepository.save(pendingParticipant);
                        
                        System.out.println("Created pending split for unregistered user: " + email + " with share: " + shareAmount);
                    } catch (Exception e) {
                        System.out.println("Error creating pending participant: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error in createParticipants: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void sendInAppNotification(SplitExpense splitExpense, User user, double shareAmount) {
        Notification notification = Notification.builder()
                .user(user)
                .title("New Split Expense")
                .message(splitExpense.getCreator().getName() + " added you to \"" + splitExpense.getTitle() + 
                        "\". Your share: ₹" + shareAmount)
                .type(Notification.NotificationType.SPLIT_CREATED)
                .relatedExpenseId(splitExpense.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private void sendEmailInvitation(SplitExpense splitExpense, String email, double shareAmount) {
        // Log invitation for unregistered users
        System.out.println("Sending email invitation to: " + email);
        System.out.println("Split: " + splitExpense.getTitle());
        System.out.println("Share Amount: ₹" + shareAmount);
        System.out.println("Creator: " + splitExpense.getCreator().getName());
        System.out.println("Invitation: Please register at our app to view and pay your split expense.");
        
        // In a real application, you would integrate with an email service like:
        // - SendGrid
        // - AWS SES
        // - JavaMail API
        // For now, we're just logging the invitation
    }

    private void sendExpenseCompletedNotification(SplitExpense splitExpense) {
        Notification notification = Notification.builder()
                .user(splitExpense.getCreator())
                .title("Expense Completed")
                .message("All participants have paid for \"" + splitExpense.getTitle() + "\"")
                .type(Notification.NotificationType.EXPENSE_COMPLETED)
                .relatedExpenseId(splitExpense.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }
}