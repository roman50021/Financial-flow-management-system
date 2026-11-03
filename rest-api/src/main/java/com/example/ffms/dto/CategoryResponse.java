package com.example.ffms.dto;

import com.example.ffms.entity.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type,
        String color,
        Long userId
) {
}
