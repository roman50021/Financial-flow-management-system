package com.example.ffms.controller;

import com.example.ffms.dto.BudgetRequest;
import com.example.ffms.dto.BudgetResponse;
import com.example.ffms.dto.BudgetStatusResponse;
import com.example.ffms.service.BudgetService;
import com.example.ffms.service.LiveUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final LiveUpdateService liveUpdateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.create(request);
        liveUpdateService.publish("budgets");
        return response;
    }

    @GetMapping
    public List<BudgetResponse> findAll(@RequestParam(required = false) Long userId) {
        return budgetService.findAll(userId);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.update(id, request);
        liveUpdateService.publish("budgets");
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        budgetService.delete(id);
        liveUpdateService.publish("budgets");
    }

    @GetMapping("/{id}/status")
    public BudgetStatusResponse checkStatus(@PathVariable Long id) {
        return budgetService.checkStatus(id);
    }
}
