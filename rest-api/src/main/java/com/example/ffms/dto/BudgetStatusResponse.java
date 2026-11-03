package com.example.ffms.dto;

import java.math.BigDecimal;

public record BudgetStatusResponse(
        Long budgetId,
        BigDecimal amountLimit,
        BigDecimal spent,
        boolean exceeded,
        String warning
) {
}
