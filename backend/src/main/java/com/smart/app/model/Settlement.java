package com.smart.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_expense_id", nullable = false)
    private SplitExpense splitExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_share_id", nullable = false)
    private SplitShare splitShare;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paymentMode;

    @Column(nullable = false)
    private LocalDateTime settledAt = LocalDateTime.now();

    @Column(length = 500)
    private String notes;
}