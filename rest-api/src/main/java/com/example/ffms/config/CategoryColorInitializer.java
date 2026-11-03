package com.example.ffms.config;

import com.example.ffms.entity.Category;
import com.example.ffms.repository.CategoryRepository;
import com.example.ffms.service.CategoryColors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryColorInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Category> categoriesWithoutColor = categoryRepository.findAll().stream()
                .filter(category -> category.getColor() == null || category.getColor().isBlank())
                .peek(category -> category.setColor(CategoryColors.forId(category.getId())))
                .toList();

        if (!categoriesWithoutColor.isEmpty()) {
            categoryRepository.saveAll(categoriesWithoutColor);
        }
    }
}
