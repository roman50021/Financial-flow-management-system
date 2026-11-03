package com.example.ffms.dto;

import com.example.ffms.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank String name,
        @NotNull TransactionType type,
        Long userId
) {
}
