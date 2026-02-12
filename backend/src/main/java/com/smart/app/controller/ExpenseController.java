package com.smart.app.controller;

import com.smart.app.model.Expense;
import com.smart.app.model.User;
import com.smart.app.repository.ExpenseRepository;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@RequestBody Expense expense) {
        User user = getCurrentUser();
        expense.setUser(user);
        return ResponseEntity.ok(expenseRepository.save(expense));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getMyExpenses() {
        User user = getCurrentUser();
        return ResponseEntity.ok(expenseRepository.findByUserIdOrderByExpenseDateDesc(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpense(@PathVariable Long id) {
        User user = getCurrentUser();
        return expenseRepository.findByIdAndUserId(id, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        User user = getCurrentUser();
        return expenseRepository.findByIdAndUserId(id, user.getId())
                .map(existingExpense -> {
                    existingExpense.setTitle(expense.getTitle());
                    existingExpense.setAmount(expense.getAmount());
                    existingExpense.setCategory(expense.getCategory());
                    existingExpense.setDescription(expense.getDescription());
                    existingExpense.setExpenseDate(expense.getExpenseDate());
                    return ResponseEntity.ok(expenseRepository.save(existingExpense));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        User user = getCurrentUser();
        if (expenseRepository.existsByIdAndUserId(id, user.getId())) {
            expenseRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}