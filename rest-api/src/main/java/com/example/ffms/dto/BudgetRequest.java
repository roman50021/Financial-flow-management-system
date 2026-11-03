package com.example.ffms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountLimit,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        Long userId,
        Long categoryId
) {
}
