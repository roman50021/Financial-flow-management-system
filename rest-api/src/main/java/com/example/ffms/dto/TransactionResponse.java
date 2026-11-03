package com.example.ffms.dto;

import com.example.ffms.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String title,
        String description,
        BigDecimal amount,
        TransactionType type,
        LocalDate transactionDate,
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long userId,
        Instant createdAt
) {
}
