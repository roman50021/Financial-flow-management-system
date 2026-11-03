package com.example.ffms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long id,
        String name,
        BigDecimal amountLimit,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long userId,
        Long categoryId,
        String categoryName
) {
}
