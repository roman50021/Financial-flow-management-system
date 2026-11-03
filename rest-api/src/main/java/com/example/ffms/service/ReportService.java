package com.example.ffms.service;

import com.example.ffms.dto.FinancialReportResponse;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.exception.BadRequestException;
import com.example.ffms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public FinancialReportResponse generate(LocalDate startDate, LocalDate endDate, Long userId) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("endDate must be after or equal to startDate");
        }

        BigDecimal totalIncome = sum(TransactionType.INCOME, userId, startDate, endDate);
        BigDecimal totalExpense = sum(TransactionType.EXPENSE, userId, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        List<String> warnings = new ArrayList<>();

        if (totalExpense.compareTo(totalIncome) > 0) {
            warnings.add("Expenses exceed income for the selected period");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Balance is negative");
        }

        return new FinancialReportResponse(totalIncome, totalExpense, balance, startDate, endDate, warnings);
    }

    private BigDecimal sum(TransactionType type, Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.search(type, null, userId, startDate, endDate)
                .stream()
                .map(transaction -> transaction.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
