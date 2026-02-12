package com.smart.app.repository;

import com.smart.app.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);
    
    Optional<Expense> findByIdAndUserId(Long id, Long userId);
    
    boolean existsByIdAndUserId(Long id, Long userId);

    List<Expense> findByUserIdAndExpenseDate(Long userId, LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.expenseDate = :date")
    Double getTotalExpenseForDate(Long userId, LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND MONTH(e.expenseDate) = MONTH(:date) AND YEAR(e.expenseDate) = YEAR(:date)")
    Double getTotalExpenseForMonth(Long userId, LocalDate date);
}
