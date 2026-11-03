package com.example.financialflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financialflow.TransactionsUiState
import com.example.financialflow.model.Category
import com.example.financialflow.model.CategoryDraft
import com.example.financialflow.model.TransactionType

@Composable
internal fun CategoriesScreen(
    uiState: TransactionsUiState,
    onOpenMenu: () -> Unit,
    onCreate: (CategoryDraft, () -> Unit) -> Unit,
    onUpdate: (Long, CategoryDraft, () -> Unit) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var editorCategory by remember { mutableStateOf<Category?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Category?>(null) }
    val filtered = remember(uiState.categories, selectedType) {
        uiState.categories.filter { it.type == selectedType }.sortedBy(Category::name)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CategoriesHeader(
                categoryCount = uiState.categories.size,
                onOpenMenu = onOpenMenu,
                onAdd = { showCreateDialog = true },
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = { Text("Категорії витрат") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = { Text("Категорії доходів") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyCategories(
                    type = selectedType,
                    onAdd = { showCreateDialog = true },
                )
            }
        } else {
            items(filtered, key = Category::id) { category ->
                CategoryRow(
                    category = category,
                    color = categoryColor(category.color, category.id),
                    isDeleting = category.id in uiState.deletingCategoryIds,
                    onEdit = { editorCategory = category },
                    onDelete = { deleteCandidate = category },
                )
            }
        }
    }

    if (showCreateDialog) {
        CategoryEditorDialog(
            title = "Нова категорія",
            initialName = "",
            initialType = selectedType,
            isSaving = uiState.isSavingCategory,
            onDismiss = { if (!uiState.isSavingCategory) showCreateDialog = false },
            onSave = { draft ->
                onCreate(draft) { showCreateDialog = false }
            },
        )
    }

    editorCategory?.let { category ->
        CategoryEditorDialog(
            title = "Редагувати категорію",
            initialName = category.name,
            initialType = category.type,
            isSaving = uiState.isSavingCategory,
            onDismiss = { if (!uiState.isSavingCategory) editorCategory = null },
            onSave = { draft ->
                onUpdate(category.id, draft) { editorCategory = null }
            },
        )
    }

    deleteCandidate?.let { category ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Видалити категорію?") },
            text = {
                Text(
                    "Категорію «${category.name}» буде видалено. " +
                        "Якщо вона використовується в операціях або бюджетах, API не дозволить видалення.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        onDelete(category.id)
                    },
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Скасувати") }
            },
        )
    }
}

@Composable
private fun CategoriesHeader(
    categoryCount: Int,
    onOpenMenu: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenMenu, contentPadding = PaddingValues(8.dp)) {
                    Text("☰", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text("Категорії", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatCategoryCount(categoryCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(onClick = onAdd) {
                    Text("+ Додати")
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    color: Color,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(14.dp).background(color, CircleShape))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (category.type == TransactionType.EXPENSE) "Витрати" else "Доходи",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onEdit, enabled = !isDeleting) {
                Text("Змінити")
            }
            TextButton(onClick = onDelete, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("×", color = MaterialTheme.colorScheme.error, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyCategories(type: TransactionType, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (type == TransactionType.EXPENSE) {
                    "Категорій витрат ще немає"
                } else {
                    "Категорій доходів ще немає"
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Створіть категорію, щоб бачити її частку на графіку",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            Button(onClick = onAdd) { Text("Створити категорію") }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialType: TransactionType,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CategoryDraft) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var type by rememberSaveable(initialType) { mutableStateOf(initialType) }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Витрата") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Дохід") },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showValidation = false
                    },
                    label = { Text("Назва категорії") },
                    singleLine = true,
                    isError = showValidation && name.isBlank(),
                    supportingText = {
                        if (showValidation && name.isBlank()) Text("Вкажіть назву")
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showValidation = true
                    if (name.isNotBlank()) {
                        onSave(CategoryDraft(name = name.trim(), type = type))
                    }
                },
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Зберегти")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Скасувати") }
        },
    )
}

private fun formatCategoryCount(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "категорій"
        count % 10 == 1 -> "категорія"
        count % 10 in 2..4 -> "категорії"
        else -> "категорій"
    }
    return "$count $word для доходів і витрат"
}
