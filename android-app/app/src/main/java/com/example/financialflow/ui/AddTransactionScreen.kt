package com.example.financialflow.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financialflow.model.Category
import com.example.financialflow.model.Transaction
import com.example.financialflow.model.TransactionDraft
import com.example.financialflow.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
internal fun AddTransactionScreen(
    initialType: TransactionType,
    transaction: Transaction?,
    categories: List<Category>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
) {
    val transactionId = transaction?.id
    val isEditing = transaction != null
    var title by rememberSaveable(transactionId) { mutableStateOf(transaction?.title.orEmpty()) }
    var description by rememberSaveable(transactionId) { mutableStateOf(transaction?.description.orEmpty()) }
    var amount by rememberSaveable(transactionId) {
        mutableStateOf(transaction?.amount?.stripTrailingZeros()?.toPlainString().orEmpty())
    }
    var type by rememberSaveable(transactionId) { mutableStateOf(transaction?.type ?: initialType) }
    var date by rememberSaveable(transactionId) {
        mutableStateOf(transaction?.transactionDate ?: LocalDate.now().toString())
    }
    var selectedCategoryId by rememberSaveable(transactionId) { mutableStateOf(transaction?.categoryId) }
    var validationMessage by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }
    val matchingCategories = remember(categories, type) {
        categories.filter { it.type == type }.sortedBy(Category::name)
    }

    LaunchedEffect(type, matchingCategories) {
        if (matchingCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = matchingCategories.firstOrNull()?.id
        }
    }

    fun submit() {
        val parsedAmount = amount.replace(',', '.').toBigDecimalOrNull()
        validationMessage = when {
            parsedAmount == null || parsedAmount <= BigDecimal.ZERO -> "Вкажіть суму більшу за нуль"
            selectedCategoryId == null -> "Оберіть категорію операції"
            !isValidDate(date) -> "Дата має бути у форматі РРРР-ММ-ДД"
            else -> null
        }
        if (validationMessage == null) {
            onSave(
                TransactionDraft(
                    title = title.trim().takeIf(String::isNotEmpty),
                    description = description.trim(),
                    amount = requireNotNull(parsedAmount),
                    type = type,
                    transactionDate = date,
                    categoryId = selectedCategoryId,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = !isSaving,
                    contentPadding = PaddingValues(8.dp),
                ) {
                    Text("←", fontSize = 27.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = if (isEditing) "Редагування операції" else "Нова операція",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isEditing) {
                            "Виправте потрібні дані та збережіть зміни"
                        } else {
                            "Оберіть категорію, вкажіть суму та збережіть"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = {
                            type = TransactionType.EXPENSE
                            selectedCategoryId = null
                        },
                        label = { Text("Витрата") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = {
                            type = TransactionType.INCOME
                            selectedCategoryId = null
                        },
                        label = { Text("Дохід") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                FormCard {
                    Text("Основне", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it.filter { char -> char.isDigit() || char in ".," }
                            validationMessage = null
                        },
                        label = { Text("Сума, ₴") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationMessage = null
                        },
                        label = { Text("Назва (необов’язково)") },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                FormCard {
                    Text("Категорія", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matchingCategories, key = Category::id) { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) },
                            )
                        }
                    }
                    if (matchingCategories.isEmpty()) {
                        Text(
                            text = "Спочатку створіть категорію цього типу через burger-меню",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                FormCard {
                    Text("Деталі", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = date,
                        onValueChange = {
                            date = it
                            validationMessage = null
                        },
                        label = { Text("Дата (РРРР-ММ-ДД)") },
                        supportingText = { Text("Автоматично встановлено сьогодні") },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Опис (необов’язково)") },
                        enabled = !isSaving,
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            validationMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Button(
                    onClick = ::submit,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = if (isEditing) "Зберегти зміни" else "Зберегти операцію",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun isValidDate(value: String): Boolean = try {
    LocalDate.parse(value)
    true
} catch (_: DateTimeParseException) {
    false
}
