package com.example.ffms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialReportResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<String> warnings
) {
}
