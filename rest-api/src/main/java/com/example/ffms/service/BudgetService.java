package com.example.ffms.service;

import com.example.ffms.dto.BudgetRequest;
import com.example.ffms.dto.BudgetResponse;
import com.example.ffms.dto.BudgetStatusResponse;
import com.example.ffms.entity.Budget;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.TransactionType;
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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialMapper mapper;

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        Budget budget = new Budget();
        apply(budget, request);
        return mapper.toBudgetResponse(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAll(Long userId) {
        List<Budget> budgets = userId == null ? budgetRepository.findAll() : budgetRepository.findByUserIdOrUserIsNull(userId);
        return budgets.stream().map(mapper::toBudgetResponse).toList();
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = findEntity(id);
        apply(budget, request);
        return mapper.toBudgetResponse(budget);
    }

    @Transactional
    public void delete(Long id) {
        budgetRepository.delete(findEntity(id));
    }

    @Transactional(readOnly = true)
    public BudgetStatusResponse checkStatus(Long id) {
        Budget budget = findEntity(id);
        Long categoryId = budget.getCategory() == null ? null : budget.getCategory().getId();
        Long userId = budget.getUser() == null ? null : budget.getUser().getId();
        BigDecimal spent = transactionRepository.search(TransactionType.EXPENSE, categoryId, userId, budget.getPeriodStart(), budget.getPeriodEnd())
                .stream()
                .map(transaction -> transaction.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean exceeded = spent.compareTo(budget.getAmountLimit()) > 0;
        String warning = exceeded ? "Budget limit exceeded" : null;
        return new BudgetStatusResponse(budget.getId(), budget.getAmountLimit(), spent, exceeded, warning);
    }

    private Budget findEntity(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private void apply(Budget budget, BudgetRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BadRequestException("periodEnd must be after or equal to periodStart");
        }
        budget.setName(request.name());
        budget.setAmountLimit(request.amountLimit());
        budget.setPeriodStart(request.periodStart());
        budget.setPeriodEnd(request.periodEnd());
        budget.setUser(resolveUser(request.userId()));
        budget.setCategory(resolveCategory(request.categoryId()));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
