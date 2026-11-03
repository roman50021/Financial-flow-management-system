package com.example.ffms.service;

import com.example.ffms.dto.BudgetStatusResponse;
import com.example.ffms.entity.Budget;
import com.example.ffms.entity.FinancialTransaction;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.mapper.FinancialMapper;
import com.example.ffms.repository.BudgetRepository;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.repository.TransactionRepository;
import com.example.ffms.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BudgetServiceTest {

    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetService budgetService = new BudgetService(
            budgetRepository,
            categoryRepository,
            userRepository,
            transactionRepository,
            new FinancialMapper()
    );

    @Test
    void checkStatusReturnsExceededBudgetWarning() {
        Budget budget = new Budget();
        budget.setId(5L);
        budget.setAmountLimit(new BigDecimal("500.00"));
        budget.setPeriodStart(LocalDate.of(2026, 1, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 1, 31));
        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(transactionRepository.search(eq(TransactionType.EXPENSE), isNull(), isNull(), eq(budget.getPeriodStart()), eq(budget.getPeriodEnd())))
                .thenReturn(List.of(transaction("300.00"), transaction("250.00")));

        BudgetStatusResponse response = budgetService.checkStatus(5L);

        assertThat(response.spent()).isEqualByComparingTo("550.00");
        assertThat(response.exceeded()).isTrue();
        assertThat(response.warning()).isEqualTo("Budget limit exceeded");
    }

    private FinancialTransaction transaction(String amount) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}
