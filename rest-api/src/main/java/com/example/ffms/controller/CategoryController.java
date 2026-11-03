package com.example.ffms.controller;

import com.example.ffms.dto.CategoryRequest;
import com.example.ffms.dto.CategoryResponse;
import com.example.ffms.service.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class   CategoryController {

    private final CategoryService categoryService;
    private final LiveUpdateService liveUpdateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        liveUpdateService.publish("categories");
        return response;
    }

    @GetMapping
    public List<CategoryResponse> findAll(@RequestParam(required = false) Long userId) {
        return categoryService.findAll(userId);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.update(id, request);
        liveUpdateService.publish("categories");
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
        liveUpdateService.publish("categories");
    }
}
