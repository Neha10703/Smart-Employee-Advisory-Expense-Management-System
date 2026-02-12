package com.smart.app.service;

import com.smart.app.config.JwtService;
import com.smart.app.controller.auth.AuthenticationRequest;
import com.smart.app.controller.auth.AuthenticationResponse;
import com.smart.app.controller.auth.RegisterRequest;
import com.smart.app.controller.auth.BankRegisterRequest;
import com.smart.app.model.*;
import com.smart.app.repository.*;
import com.smart.app.service.IFSCVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PendingSplitParticipantRepository pendingSplitParticipantRepository;
    private final SplitParticipantRepository splitParticipantRepository;
    private final NotificationRepository notificationRepository;
    private final IFSCVerificationService ifscVerificationService;
    private final DocumentService documentService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if email already exists
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered. Please use a different email or login.");
        }
        
        var user = User.builder()
                .name(request.getFirstname() + " " + request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() == null ? Role.USER : request.getRole())
                .phone(request.getPhone())
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(passwordEncoder.encode(request.getSecurityAnswer()))
                .createdAt(java.time.LocalDateTime.now())
                .build();
        repository.save(user);
        
        // Create sample documents for new user (with error handling)
        try {
            documentService.createSampleDocuments(user);
        } catch (Exception e) {
            System.err.println("Failed to create sample documents: " + e.getMessage());
        }
        
        // Process any pending splits for this email
        processPendingSplits(user);
        
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse registerBank(BankRegisterRequest request) {
        // Check if email already exists
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered. Please use a different email or login.");
        }
        
        // Verify IFSC code format
        if (!ifscVerificationService.verifyIFSC(request.getIfscCode())) {
            throw new RuntimeException("Invalid IFSC code format");
        }
        
        // Try to get bank name from IFSC, fallback to provided name
        String verifiedBankName = ifscVerificationService.getBankName(request.getIfscCode());
        
        var user = User.builder()
                .name(request.getContactPerson())
                .bankName(verifiedBankName != null ? verifiedBankName : request.getBankName())
                .ifscCode(request.getIfscCode())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.BANK)
                .phone(request.getPhone())
                .address(request.getAddress())
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(passwordEncoder.encode(request.getSecurityAnswer()))
                .createdAt(java.time.LocalDateTime.now())
                .build();
        repository.save(user);
        
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        
        // Process any pending splits for this email
        processPendingSplits(user);
        
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public String getSecurityQuestion(String email) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
            throw new RuntimeException("No security question set for this user");
        }
        
        return user.getSecurityQuestion();
    }

    public boolean verifySecurityAnswer(String email, String answer) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getSecurityAnswer() == null || user.getSecurityAnswer().isEmpty()) {
            throw new RuntimeException("No security answer set for this user");
        }
        
        return passwordEncoder.matches(answer, user.getSecurityAnswer());
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        var user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }
    
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public boolean emailExists(String email) {
        return repository.findByEmail(email).isPresent();
    }
    
    private void processPendingSplits(User user) {
        List<PendingSplitParticipant> pendingParticipants = pendingSplitParticipantRepository.findByEmailAndNotifiedFalse(user.getEmail());
        
        for (PendingSplitParticipant pending : pendingParticipants) {
            // Create actual participant record
            SplitParticipant participant = SplitParticipant.builder()
                    .splitExpense(pending.getSplitExpense())
                    .user(user)
                    .shareAmount(pending.getShareAmount())
                    .paymentStatus(SplitParticipant.PaymentStatus.PENDING)
                    .build();
            splitParticipantRepository.save(participant);
            
            // Create notification
            Notification notification = Notification.builder()
                    .user(user)
                    .title("New Split Expense")
                    .message(pending.getSplitExpense().getCreator().getName() + " added you to \"" + pending.getSplitExpense().getTitle() + 
                            "\". Your share: ₹" + pending.getShareAmount())
                    .type(Notification.NotificationType.SPLIT_CREATED)
                    .relatedExpenseId(pending.getSplitExpense().getId())
                    .isRead(false)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            
            // Mark as notified
            pending.setNotified(true);
            pendingSplitParticipantRepository.save(pending);
        }
    }
}
