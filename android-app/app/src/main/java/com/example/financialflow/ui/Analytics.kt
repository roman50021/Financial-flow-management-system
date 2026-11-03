package com.example.financialflow.ui

import androidx.compose.ui.graphics.Color
import com.example.financialflow.model.Transaction
import com.example.financialflow.model.TransactionType
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal val ChartPalette = listOf(
    Color(0xFF7C5CFC),
    Color(0xFFFF5C7A),
    Color(0xFF3B82F6),
    Color(0xFFF59E0B),
    Color(0xFFA855F7),
    Color(0xFF64748B),
    Color(0xFFEC4899),
)

internal data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

internal data class ChartSlice(
    val categoryId: Long?,
    val name: String,
    val amount: BigDecimal,
    val color: Color,
)

internal enum class AnalyticsPeriod(val label: String) {
    DAY("День"),
    WEEK("Тиждень"),
    MONTH("Місяць"),
    YEAR("Рік");

    fun rangeForOffset(offset: Int, today: LocalDate = LocalDate.now()): DateRange {
        val anchor = when (this) {
            DAY -> today.plusDays(offset.toLong())
            WEEK -> today.plusWeeks(offset.toLong())
            MONTH -> today.plusMonths(offset.toLong())
            YEAR -> today.plusYears(offset.toLong())
        }
        return when (this) {
            DAY -> DateRange(anchor, anchor)
            WEEK -> {
                val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                DateRange(monday, monday.plusDays(6))
            }
            MONTH -> {
                val first = anchor.withDayOfMonth(1)
                DateRange(first, first.withDayOfMonth(first.lengthOfMonth()))
            }
            YEAR -> DateRange(
                LocalDate.of(anchor.year, 1, 1),
                LocalDate.of(anchor.year, 12, 31),
            )
        }
    }

    fun displayName(range: DateRange): String {
        val locale = Locale.forLanguageTag("uk-UA")
        val value = when (this) {
            DAY -> range.start.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
            WEEK -> "${range.start.format(DateTimeFormatter.ofPattern("d MMM", locale))} — " +
                range.end.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
            MONTH -> range.start.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
            YEAR -> range.start.year.toString()
        }
        return value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

internal fun transactionsFor(
    transactions: List<Transaction>,
    type: TransactionType,
    range: DateRange,
): List<Transaction> = transactions.filter { transaction ->
    transaction.type == type && runCatching {
        LocalDate.parse(transaction.transactionDate) in range
    }.getOrDefault(false)
}

internal fun chartSlices(transactions: List<Transaction>): List<ChartSlice> = transactions
    .groupBy { Triple(it.categoryId, it.categoryName ?: "Без категорії", it.categoryColor) }
    .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { total, item -> total + item.amount } }
    .entries
    .sortedByDescending { it.value }
    .mapIndexed { index, entry ->
        ChartSlice(
            categoryId = entry.key.first,
            name = entry.key.second,
            amount = entry.value,
            color = categoryColor(entry.key.third, entry.key.first, index),
        )
    }

internal fun categoryColor(value: String?, categoryId: Long? = null, fallbackIndex: Int = 0): Color {
    val storedColor = value?.let { color ->
        runCatching { Color(android.graphics.Color.parseColor(color)) }.getOrNull()
    }
    if (storedColor != null) {
        return storedColor
    }

    val index = categoryId
        ?.let { Math.floorMod(it - 1, ChartPalette.size.toLong()).toInt() }
        ?: Math.floorMod(fallbackIndex, ChartPalette.size)
    return ChartPalette[index]
}

internal fun formatMoney(value: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("uk-UA")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    return "${formatter.format(value)} ₴"
}

internal fun formatShortDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("uk-UA")))
}.getOrDefault(value)

internal fun formatTransactionTime(value: String?): String? = value?.let { timestamp ->
    runCatching {
        Instant.parse(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("uk-UA")))
    }.getOrNull()
}

internal fun formatOperationCount(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> "операцій"
        count % 10 == 1 -> "операція"
        count % 10 in 2..4 -> "операції"
        else -> "операцій"
    }
    return "$count $word"
}
