package com.example.ffms.service;

import com.example.ffms.dto.CategoryRequest;
import com.example.ffms.dto.CategoryResponse;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.User;
import com.example.ffms.exception.BadRequestException;
import com.example.ffms.exception.ResourceNotFoundException;
import com.example.ffms.mapper.FinancialMapper;
import com.example.ffms.repository.BudgetRepository;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.repository.TransactionRepository;
import com.example.ffms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialMapper mapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        categoryRepository.save(category);
        category.setColor(CategoryColors.forId(category.getId()));
        return mapper.toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(Long userId) {
        List<Category> categories = userId == null
                ? categoryRepository.findAll()
                : categoryRepository.findByUserIdOrUserIsNull(userId);
        return categories.stream().map(mapper::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntity(id);
        if (category.getType() != request.type() && isInUse(id)) {
            throw new BadRequestException(
                    "Тип категорії не можна змінити, доки вона використовується в операціях або бюджетах."
            );
        }
        apply(category, request);
        return mapper.toCategoryResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findEntity(id);
        if (isInUse(id)) {
            throw new BadRequestException(
                    "Категорію не можна видалити, доки вона використовується в операціях або бюджетах."
            );
        }
        categoryRepository.delete(category);
    }

    private boolean isInUse(Long id) {
        return transactionRepository.existsByCategoryId(id) || budgetRepository.existsByCategoryId(id);
    }

    Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setType(request.type());
        category.setUser(resolveUser(request.userId()));
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
