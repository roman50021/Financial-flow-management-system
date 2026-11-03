package com.example.ffms.repository;

import com.example.ffms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrUserIsNull(Long userId);
}
