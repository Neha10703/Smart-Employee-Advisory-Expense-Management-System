package com.smart.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "split_shares")
public class SplitShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_expense_id", nullable = false)
    private SplitExpense splitExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(nullable = false)
    private Double owedAmount;

    @Column(nullable = false)
    private Double paidAmount = 0.0;

    @Column(nullable = false)
    private Double percentage = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareStatus status = ShareStatus.PENDING;

    public enum ShareStatus {
        PENDING, SETTLED
    }

    public String getParticipantName() {
        return user != null ? user.getName() : guestName;
    }

    public String getParticipantEmail() {
        return user != null ? user.getEmail() : guestEmail;
    }
}