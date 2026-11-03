package com.example.ffms.dto;

import com.example.ffms.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        String title,
        String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull TransactionType type,
        @NotNull LocalDate transactionDate,
        @NotNull Long categoryId,
        Long userId
) {
}
