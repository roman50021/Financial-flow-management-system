package com.example.ffms.mapper;

import com.example.ffms.dto.BudgetResponse;
import com.example.ffms.dto.CategoryResponse;
import com.example.ffms.dto.TransactionResponse;
import com.example.ffms.entity.Budget;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.FinancialTransaction;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class FinancialMapper {

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getUser() == null ? null : category.getUser().getId()
        );
    }

    public TransactionResponse toTransactionResponse(FinancialTransaction transaction) {
        Category category = transaction.getCategory();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTitle(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTransactionDate(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getColor(),
                transaction.getUser() == null ? null : transaction.getUser().getId(),
                transaction.getCreatedAt() == null
                        ? null
                        : transaction.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }

    public BudgetResponse toBudgetResponse(Budget budget) {
        Category category = budget.getCategory();
        return new BudgetResponse(
                budget.getId(),
                budget.getName(),
                budget.getAmountLimit(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                budget.getUser() == null ? null : budget.getUser().getId(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName()
        );
    }
}
