package com.example.financialflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financialflow.TransactionsUiState
import com.example.financialflow.model.Transaction
import com.example.financialflow.model.TransactionType
import java.math.BigDecimal
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val PagerCenter = 1_200
private const val PagerCount = PagerCenter + 1
private val ExpenseColor = Color(0xFFFF5C7A)
private val IncomeColor = Color(0xFF6D8BFF)

@Composable
internal fun DashboardScreen(
    uiState: TransactionsUiState,
    onOpenMenu: () -> Unit,
    onAddTransaction: (TransactionType) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var selectedPeriod by rememberSaveable { mutableStateOf(AnalyticsPeriod.MONTH) }
    val pagerState = rememberPagerState(initialPage = PagerCenter, pageCount = { PagerCount })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedPeriod) {
        pagerState.scrollToPage(PagerCenter)
    }

    val currentRange = selectedPeriod.rangeForOffset(pagerState.currentPage - PagerCenter)
    val currentTransactions = remember(
        uiState.transactions,
        selectedType,
        currentRange,
    ) {
        transactionsFor(uiState.transactions, selectedType, currentRange)
    }
    val currentSlices = remember(currentTransactions) { chartSlices(currentTransactions) }
    val overallBalance = remember(uiState.transactions) {
        uiState.transactions.fold(BigDecimal.ZERO) { balance, transaction ->
            if (transaction.type == TransactionType.INCOME) {
                balance + transaction.amount
            } else {
                balance - transaction.amount
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DashboardHeader(
                balance = overallBalance,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onOpenMenu = onOpenMenu,
            )
        }

        item {
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        item {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val range = selectedPeriod.rangeForOffset(page - PagerCenter)
                val transactions = transactionsFor(uiState.transactions, selectedType, range)
                AnalyticsCard(
                    title = selectedPeriod.displayName(range),
                    type = selectedType,
                    transactions = transactions,
                    onPrevious = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    onNext = if (page < PagerCenter) {
                        {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(PagerCenter))
                            }
                        }
                    } else {
                        null
                    },
                    onAdd = { onAddTransaction(selectedType) },
                )
            }
        }

        item {
            SectionTitle(
                title = if (selectedType == TransactionType.EXPENSE) "За категоріями" else "Джерела доходу",
                subtitle = selectedPeriod.displayName(currentRange),
            )
        }

        if (currentSlices.isEmpty()) {
            item {
                EmptyAnalytics(
                    type = selectedType,
                    onAdd = { onAddTransaction(selectedType) },
                )
            }
        } else {
            items(currentSlices, key = { "${it.categoryId}:${it.name}" }) { slice ->
                CategoryAmountRow(
                    slice = slice,
                    total = currentSlices.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount },
                )
            }
        }

        if (currentTransactions.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Операції періоду",
                    subtitle = formatOperationCount(currentTransactions.size),
                )
            }
            items(currentTransactions, key = Transaction::id) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    isDeleting = transaction.id in uiState.deletingIds,
                    onEdit = { onEditTransaction(transaction) },
                    onDelete = { onDeleteTransaction(transaction.id) },
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    balance: BigDecimal,
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    onOpenMenu: () -> Unit,
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
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpenMenu, contentPadding = PaddingValues(8.dp)) {
                    Text("☰", color = MaterialTheme.colorScheme.onBackground, fontSize = 26.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Основний гаманець",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoney(balance),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TypeTab(
                    text = "ВИТРАТИ",
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { onTypeSelected(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f),
                )
                TypeTab(
                    text = "ДОХОДИ",
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { onTypeSelected(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TypeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(3.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnalyticsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label, maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    type: TransactionType,
    transactions: List<Transaction>,
    onPrevious: () -> Unit,
    onNext: (() -> Unit)?,
    onAdd: () -> Unit,
) {
    val slices = remember(transactions) { chartSlices(transactions) }
    val total = remember(transactions) {
        transactions.fold(BigDecimal.ZERO) { sum, transaction -> sum + transaction.amount }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPrevious) {
                    Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp)
                }
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                if (onNext != null) {
                    TextButton(onClick = onNext) {
                        Text("›", color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp)
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp),
                contentAlignment = Alignment.Center,
            ) {
                DonutChart(
                    slices = slices,
                    total = total,
                    type = type,
                )
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(58.dp),
                ) {
                    Text("+", fontSize = 30.sp, fontWeight = FontWeight.Light)
                }
            }

            Text(
                text = "Свайпніть, щоб змінити період",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<ChartSlice>,
    total: BigDecimal,
    type: TransactionType,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(226.dp)) {
            val strokeWidth = 34.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(
                x = (size.width - radius * 2f) / 2f,
                y = (size.height - radius * 2f) / 2f,
            )
            val chartSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

            if (total <= BigDecimal.ZERO || slices.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = chartSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            } else {
                var startAngle = -90f
                slices.forEach { slice ->
                    val rawSweep = (slice.amount.toDouble() / total.toDouble() * 360.0).toFloat()
                    val gap = if (rawSweep > 4f) 2.5f else 0.5f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + gap / 2f,
                        sweepAngle = (rawSweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = chartSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                    startAngle += rawSweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (type == TransactionType.EXPENSE) "Витрати" else "Доходи",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            AutoFitAmount(
                value = total,
                color = if (type == TransactionType.EXPENSE) ExpenseColor else IncomeColor,
            )
        }
    }
}

@Composable
private fun AutoFitAmount(
    value: BigDecimal,
    color: Color,
) {
    val text = formatMoney(value)
    var fontSize by remember(text) { mutableStateOf(25.sp) }

    Text(
        modifier = Modifier.width(144.dp),
        text = text,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * 1.12f,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > 12f) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(12f).sp
            }
        },
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryAmountRow(slice: ChartSlice, total: BigDecimal) {
    val percent = if (total > BigDecimal.ZERO) {
        (slice.amount.toDouble() / total.toDouble() * 100).roundToInt()
    } else {
        0
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
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
                    .size(42.dp)
                    .background(slice.color.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(13.dp).background(slice.color, CircleShape))
            }
            Text(
                text = slice.name,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$percent%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(formatMoney(slice.amount), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyAnalytics(type: TransactionType, onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (type == TransactionType.EXPENSE) {
                    "Витрат за цей період немає"
                } else {
                    "Доходів за цей період немає"
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Додайте операцію — графік оновиться автоматично",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp),
            )
            Button(onClick = onAdd) { Text("Додати операцію") }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val customTitle = transaction.title?.takeIf(String::isNotBlank)
    val displayTitle = customTitle ?: transaction.categoryName ?: "Операція"
    val metadata = buildList {
        if (customTitle != null) {
            transaction.categoryName?.let(::add)
        }
        add(formatShortDate(transaction.transactionDate))
        formatTransactionTime(transaction.createdAt)?.let(::add)
    }.joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(displayTitle, fontWeight = FontWeight.SemiBold)
                Text(
                    text = metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = formatMoney(transaction.amount),
                color = if (transaction.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = onEdit,
                enabled = !isDeleting,
                modifier = Modifier.size(42.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                Text(
                    text = "✎",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 21.sp,
                )
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
