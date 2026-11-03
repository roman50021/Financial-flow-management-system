package com.example.ffms.service;

import com.example.ffms.dto.TransactionRequest;
import com.example.ffms.dto.TransactionResponse;
import com.example.ffms.entity.Category;
import com.example.ffms.entity.FinancialTransaction;
import com.example.ffms.entity.TransactionType;
import com.example.ffms.entity.User;
import com.example.ffms.exception.BadRequestException;
import com.example.ffms.exception.ResourceNotFoundException;
import com.example.ffms.mapper.FinancialMapper;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.repository.TransactionRepository;
import com.example.ffms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FinancialMapper mapper;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        FinancialTransaction transaction = new FinancialTransaction();
        apply(transaction, request);
        return mapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll(TransactionType type, Long categoryId, Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.search(type, categoryId, userId, startDate, endDate)
                .stream()
                .map(mapper::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return mapper.toTransactionResponse(findEntity(id));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        FinancialTransaction transaction = findEntity(id);
        apply(transaction, request);
        return mapper.toTransactionResponse(transaction);
    }

    @Transactional
    public void delete(Long id) {
        transactionRepository.delete(findEntity(id));
    }

    FinancialTransaction findEntity(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    private void apply(FinancialTransaction transaction, TransactionRequest request) {
        transaction.setTitle(normalizeOptionalText(request.title()));
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setCategory(resolveCategory(request.categoryId(), request.type()));
        transaction.setUser(resolveUser(request.userId()));
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Category resolveCategory(Long categoryId, TransactionType transactionType) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        if (category.getType() != transactionType) {
            throw new BadRequestException("Тип категорії має відповідати типу операції.");
        }
        return category;
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
