package com.example.financialflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financialflow.data.ApiException
import com.example.financialflow.data.TransactionRepository
import com.example.financialflow.model.Category
import com.example.financialflow.model.CategoryDraft
import com.example.financialflow.model.Transaction
import com.example.financialflow.model.TransactionDraft
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingCategory: Boolean = false,
    val deletingIds: Set<Long> = emptySet(),
    val deletingCategoryIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
)

class MainViewModel(
    private val repository: TransactionRepository = TransactionRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            synchronize(showLoading = true, reportErrors = true)
        }
        viewModelScope.launch {
            repository.observeDataChanges().collect {
                synchronize(showLoading = false, reportErrors = false)
            }
        }
    }

    private suspend fun synchronize(showLoading: Boolean, reportErrors: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        try {
            val (transactions, categories) = coroutineScope {
                val transactions = async { repository.getTransactions() }
                val categories = async { repository.getCategories() }
                transactions.await() to categories.await()
            }
            _uiState.update {
                it.copy(
                    transactions = transactions,
                    categories = categories,
                    isLoading = false,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (reportErrors) error.toUserMessage() else it.errorMessage,
                )
            }
        }
    }

    fun addTransaction(draft: TransactionDraft, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { repository.createTransaction(draft) }
                .onSuccess { transaction ->
                    _uiState.update {
                        it.copy(
                            transactions = listOf(transaction) + it.transactions.filterNot { current ->
                                current.id == transaction.id
                            },
                            isSaving = false,
                            noticeMessage = "Операцію додано",
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun updateTransaction(id: Long, draft: TransactionDraft, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { repository.updateTransaction(id, draft) }
                .onSuccess { transaction ->
                    _uiState.update { state ->
                        state.copy(
                            transactions = state.transactions.map { current ->
                                if (current.id == id) transaction else current
                            },
                            isSaving = false,
                            noticeMessage = "Операцію оновлено",
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(deletingIds = it.deletingIds + id, errorMessage = null)
            }
            runCatching { repository.deleteTransaction(id) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            transactions = state.transactions.filterNot { it.id == id },
                            deletingIds = state.deletingIds - id,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            deletingIds = it.deletingIds - id,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun addCategory(draft: CategoryDraft, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCategory = true, errorMessage = null) }
            runCatching { repository.createCategory(draft) }
                .onSuccess { category ->
                    _uiState.update {
                        it.copy(
                            categories = (it.categories.filterNot { current -> current.id == category.id } + category)
                                .sortedBy(Category::name),
                            isSavingCategory = false,
                            noticeMessage = "Категорію створено",
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSavingCategory = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun updateCategory(id: Long, draft: CategoryDraft, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCategory = true, errorMessage = null) }
            runCatching { repository.updateCategory(id, draft) }
                .onSuccess { category ->
                    _uiState.update {
                        it.copy(
                            categories = it.categories
                                .map { current -> if (current.id == id) category else current }
                                .sortedBy(Category::name),
                            transactions = it.transactions.map { transaction ->
                                if (transaction.categoryId == id) {
                                    transaction.copy(
                                        categoryName = category.name,
                                        categoryColor = category.color,
                                    )
                                } else {
                                    transaction
                                }
                            },
                            isSavingCategory = false,
                            noticeMessage = "Категорію оновлено",
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSavingCategory = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingCategoryIds = it.deletingCategoryIds + id,
                    errorMessage = null,
                )
            }
            runCatching { repository.deleteCategory(id) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            categories = it.categories.filterNot { category -> category.id == id },
                            deletingCategoryIds = it.deletingCategoryIds - id,
                            noticeMessage = "Категорію видалено",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            deletingCategoryIds = it.deletingCategoryIds - id,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }
}

private fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> message
    is SocketTimeoutException -> "API не відповідає. Спробуйте ще раз."
    is ConnectException, is UnknownHostException ->
        "Не вдалося підключитися до API за адресою ${BuildConfig.API_BASE_URL}."
    else -> message ?: "Сталася невідома помилка."
}
