package com.example.ffms.service;

import com.example.ffms.dto.TransactionRequest;
import com.example.ffms.dto.TransactionResponse;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.FinancialTransaction;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.mapper.FinancialMapper;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.repository.TransactionRepository;
import com.example.ffms.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TransactionService transactionService = new TransactionService(
            transactionRepository,
            categoryRepository,
            userRepository,
            new FinancialMapper()
    );

    @Test
    void createAllowsBlankTitleAndUsesCategory() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Продукти");
        category.setType(TransactionType.EXPENSE);
        category.setColor("#EC4899");
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(FinancialTransaction.class))).thenAnswer(invocation -> {
            FinancialTransaction transaction = invocation.getArgument(0);
            transaction.setId(11L);
            return transaction;
        });

        TransactionResponse response = transactionService.create(new TransactionRequest(
                "   ",
                null,
                new BigDecimal("125.50"),
                TransactionType.EXPENSE,
                LocalDate.of(2026, 10, 24),
                7L,
                null
        ));

        assertThat(response.title()).isNull();
        assertThat(response.categoryId()).isEqualTo(7L);
        assertThat(response.categoryName()).isEqualTo("Продукти");
        assertThat(response.categoryColor()).isEqualTo("#EC4899");
    }
}
