package com.example.ffms.service;

import com.example.ffms.dto.FinancialReportResponse;
import com.example.ffms.entity.FinancialTransaction;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.exception.BadRequestException;
import com.example.ffms.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final ReportService reportService = new ReportService(transactionRepository);

    @Test
    void generateCalculatesIncomeExpenseBalanceAndWarnings() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(transactionRepository.search(eq(TransactionType.INCOME), isNull(), eq(1L), eq(start), eq(end)))
                .thenReturn(List.of(transaction("1000.00")));
        when(transactionRepository.search(eq(TransactionType.EXPENSE), isNull(), eq(1L), eq(start), eq(end)))
                .thenReturn(List.of(transaction("1200.00")));

        FinancialReportResponse report = reportService.generate(start, end, 1L);

        assertThat(report.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(report.totalExpense()).isEqualByComparingTo("1200.00");
        assertThat(report.balance()).isEqualByComparingTo("-200.00");
        assertThat(report.warnings()).contains("Expenses exceed income for the selected period", "Balance is negative");
    }

    @Test
    void generateRejectsInvalidPeriod() {
        assertThatThrownBy(() -> reportService.generate(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(BadRequestException.class);
    }

    private FinancialTransaction transaction(String amount) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}
