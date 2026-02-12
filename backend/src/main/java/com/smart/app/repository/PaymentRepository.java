package com.smart.app.repository;

import com.smart.app.model.Payment;
import com.smart.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser(User user);
    List<Payment> findByUserId(Long userId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
}