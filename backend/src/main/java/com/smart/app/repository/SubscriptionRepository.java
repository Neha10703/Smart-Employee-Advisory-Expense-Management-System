package com.smart.app.repository;

import com.smart.app.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}