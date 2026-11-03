package com.example.financialflow.model

import java.math.BigDecimal

enum class TransactionType {
    INCOME,
    EXPENSE,
}

data class Transaction(
    val id: Long,
    val title: String?,
    val description: String?,
    val amount: BigDecimal,
    val type: TransactionType,
    val transactionDate: String,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: String?,
    val createdAt: String?,
)

data class TransactionDraft(
    val title: String?,
    val description: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val transactionDate: String,
    val categoryId: Long?,
)

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val color: String?,
)

data class CategoryDraft(
    val name: String,
    val type: TransactionType,
)
