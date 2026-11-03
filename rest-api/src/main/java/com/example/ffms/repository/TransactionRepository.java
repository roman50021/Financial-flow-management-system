package com.example.ffms.repository;

import com.example.ffms.entity.FinancialTransaction;
import com.example.ffms.entity.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long>, JpaSpecificationExecutor<FinancialTransaction> {

    boolean existsByCategoryId(Long categoryId);

    default List<FinancialTransaction> search(TransactionType type, Long categoryId, Long userId,
                                             LocalDate startDate, LocalDate endDate) {
        // Omit absent filters so PostgreSQL never needs to infer a null parameter's type.
        return findAll((root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (type != null) {
                filters.add(builder.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                filters.add(builder.equal(root.get("category").get("id"), categoryId));
            }
            if (userId != null) {
                filters.add(builder.equal(root.get("user").get("id"), userId));
            }
            if (startDate != null) {
                filters.add(builder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                filters.add(builder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.DESC, "transactionDate", "id"));
    }
}
