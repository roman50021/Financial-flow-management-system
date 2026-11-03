package com.example.ffms.service;

import com.example.ffms.dto.CategoryRequest;
import com.example.ffms.dto.CategoryResponse;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.mapper.FinancialMapper;
import com.example.ffms.repository.BudgetRepository;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.repository.TransactionRepository;
import com.example.ffms.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final CategoryService categoryService = new CategoryService(
            categoryRepository,
            userRepository,
            transactionRepository,
            budgetRepository,
            new FinancialMapper()
    );

    @Test
    void createStoresCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(10L);
            return category;
        });

        CategoryResponse response = categoryService.create(new CategoryRequest("Sales", TransactionType.INCOME, null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Sales");
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
        assertThat(response.color()).isEqualTo("#3B82F6");
    }
}
