package com.smart.app.service;

import com.smart.app.model.Budget;
import com.smart.app.model.Expense;
import com.smart.app.model.User;
import com.smart.app.repository.BudgetRepository;
import com.smart.app.repository.ExpenseRepository;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    public Expense addExpense(Expense expense) {
        User user = getCurrentUser();
        expense.setUser(user);
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        return expenseRepository.save(expense);
    }

    public List<Expense> getExpensesByDate(LocalDate date) {
        return expenseRepository.findByUserIdAndExpenseDate(getCurrentUser().getId(), date);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(getCurrentUser().getId());
    }

    public Budget setBudget(Budget budget) {
        User user = getCurrentUser();
        Budget existing = budgetRepository.findByUserId(user.getId()).orElse(Budget.builder().user(user).build());
        existing.setDailyLimit(budget.getDailyLimit());
        existing.setMonthlyLimit(budget.getMonthlyLimit());
        return budgetRepository.save(existing);
    }

    public Budget getBudget() {
        return budgetRepository.findByUserId(getCurrentUser().getId()).orElse(null);
    }

    public Map<String, Object> getDashboardStats() {
        User user = getCurrentUser();
        Double dailyTotal = expenseRepository.getTotalExpenseForDate(user.getId(), LocalDate.now());
        Double monthlyTotal = expenseRepository.getTotalExpenseForMonth(user.getId(), LocalDate.now());

        Budget budget = budgetRepository.findByUserId(user.getId()).orElse(null);

        boolean dailyAlert = budget != null && budget.getDailyLimit() != null && dailyTotal != null
                && dailyTotal > budget.getDailyLimit();
        boolean monthlyAlert = budget != null && budget.getMonthlyLimit() != null && monthlyTotal != null
                && monthlyTotal > budget.getMonthlyLimit();

        return Map.of(
                "dailyTotal", dailyTotal == null ? 0.0 : dailyTotal,
                "monthlyTotal", monthlyTotal == null ? 0.0 : monthlyTotal,
                "dailyLimit", budget != null ? budget.getDailyLimit() : 0.0,
                "monthlyLimit", budget != null ? budget.getMonthlyLimit() : 0.0,
                "dailyAlert", dailyAlert,
                "monthlyAlert", monthlyAlert);
    }
}
