package com.example.ffms.repository;

import com.example.ffms.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    boolean existsByCategoryId(Long categoryId);

    List<Budget> findByUserIdOrUserIsNull(Long userId);
}
