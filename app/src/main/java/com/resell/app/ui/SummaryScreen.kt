package com.resell.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resell.app.data.AppScreen
import com.resell.app.data.formatDateForDisplay
import com.resell.app.data.PlatformType
import com.resell.app.data.Product
import com.resell.app.data.hasActivityBetween
import com.resell.app.data.parseAmount
import com.resell.app.data.parseDateOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class PlatformSummary(
    val products: Int,
    val soldAmount: Double
)

private data class SummaryStats(
    val totalListed: Int,
    val totalSoldAmount: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val net: Double,
    val vinted: PlatformSummary,
    val ebay: PlatformSummary,
    val etsy: PlatformSummary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    products: List<Product>,
    onSelectScreen: (AppScreen) -> Unit
) {
    val today = remember { LocalDate.now() }
    var startDate by rememberSaveable { mutableStateOf(formatDateForDisplay(today.withDayOfMonth(1))) }
    var endDate by rememberSaveable { mutableStateOf(formatDateForDisplay(today)) }
    var selectedYear by rememberSaveable { mutableIntStateOf(today.year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val start = parseDateOrNull(startDate) ?: today.withDayOfMonth(1)
    val end = parseDateOrNull(endDate) ?: today
    val stats = remember(products, start, end) { calculateStats(products, start, end) }
    val years = remember(today.year) { (today.year - 5..today.year + 2).toList().reversed() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Summary", style = MaterialTheme.typography.titleLarge)
                    Text("Revenue, listings, and expenses at a glance", style = MaterialTheme.typography.labelMedium, color = MutedInk)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScreenSelector(current = AppScreen.SUMMARY, onSelected = onSelectScreen, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select dates", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DateField("Start date", startDate, showBorder = true, modifier = Modifier.weight(1f)) { startDate = it }
                            DateField("End date", endDate, showBorder = true, modifier = Modifier.weight(1f)) { endDate = it }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FilledTonalButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { yearExpanded = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = BrandPurple,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(selectedYear.toString())
                                }
                                DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                                    years.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString()) },
                                            onClick = {
                                                selectedYear = year
                                                yearExpanded = false
                                                val range = YearMonth.of(selectedYear, selectedMonth)
                                                startDate = formatDateForDisplay(range.atDay(1))
                                                endDate = if (selectedYear == today.year && selectedMonth == today.monthValue) {
                                                    formatDateForDisplay(today)
                                                } else {
                                                    formatDateForDisplay(range.atEndOfMonth())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FilledTonalButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { monthExpanded = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = BrandPurple,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        YearMonth.of(selectedYear, selectedMonth).month.getDisplayName(
                                            TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    )
                                }
                                DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                                    (1..12).forEach { month ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    YearMonth.of(selectedYear, month).month.getDisplayName(
                                                        TextStyle.FULL,
                                                        Locale.getDefault()
                                                    )
                                                )
                                            },
                                            onClick = {
                                                selectedMonth = month
                                                monthExpanded = false
                                                val range = YearMonth.of(selectedYear, selectedMonth)
                                                startDate = formatDateForDisplay(range.atDay(1))
                                                endDate = if (selectedYear == today.year && selectedMonth == today.monthValue) {
                                                    formatDateForDisplay(today)
                                                } else {
                                                    formatDateForDisplay(range.atEndOfMonth())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedYear = today.year
                                    selectedMonth = today.monthValue
                                    startDate = formatDateForDisplay(LocalDate.of(2026, 1, 1))
                                    endDate = formatDateForDisplay(today)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandPurple,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("All time")
                            }
                        }
                    }
                }
            }
            item { SummaryMetricCard("Total products listed", stats.totalListed.toString(), Ink) }
            item { TotalSummaryCard(stats = stats) }
            item { PlatformSummaryCard("Vinted", stats.vinted, BrandGreen) }
            item { PlatformSummaryCard("eBay", stats.ebay, BrandBlue) }
            item { PlatformSummaryCard("Etsy", stats.etsy, BrandOrange) }
        }
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String, accent: Color) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TotalSummaryCard(stats: SummaryStats) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Total", style = MaterialTheme.typography.labelLarge, color = BrandPurple)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TotalItem("Sold", formatCurrency(stats.totalSoldAmount), BrandPurple)
                TotalItem("Purchases", formatCurrency(stats.totalPurchases), BrandPurple)
                TotalItem("Expenses", formatCurrency(stats.totalExpenses), BrandPurple)
                TotalItem("Net", formatCurrency(stats.net), BrandPurple)
            }
        }
    }
}

@Composable
private fun TotalItem(label: String, value: String, accent: Color) {
    Column(
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlatformSummaryCard(title: String, summary: PlatformSummary, accent: Color) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TotalItem("Sold", formatCurrency(summary.soldAmount), accent)
                TotalItem("Products", summary.products.toString(), MutedInk)
            }
        }
    }
}

private fun calculateStats(products: List<Product>, start: LocalDate, end: LocalDate): SummaryStats {
    val activeProducts = products.filterNot { it.deleted }

    val totalListed = activeProducts.count { product ->
        val hasListingInRange = product.platforms.any { listing ->
            parseDateOrNull(listing.dateListed)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
        }
        hasListingInRange && product.platforms.none { it.sold }
    }

    val vinted = platformSummary(activeProducts, PlatformType.VINTED, start, end)
    val ebay = platformSummary(activeProducts, PlatformType.EBAY, start, end)
    val etsy = platformSummary(activeProducts, PlatformType.ETSY, start, end)

    val totalSoldAmount = vinted.soldAmount + ebay.soldAmount + etsy.soldAmount
    val productsInRange = activeProducts.filter { it.hasActivityBetween(start, end) }
    val totalPurchases = productsInRange.sumOf { parseAmount(it.purchasePrice) }
    val totalExpenses = productsInRange.sumOf { parseAmount(it.expenses) }

    return SummaryStats(
        totalListed = totalListed,
        totalSoldAmount = totalSoldAmount,
        totalPurchases = totalPurchases,
        totalExpenses = totalExpenses,
        net = totalSoldAmount - totalPurchases - totalExpenses,
        vinted = vinted,
        ebay = ebay,
        etsy = etsy
    )
}

private fun platformSummary(
    products: List<Product>,
    platform: PlatformType,
    start: LocalDate,
    end: LocalDate
): PlatformSummary {
    val sales = products
        .flatMap { it.platforms.filter { listing -> listing.platform == platform && listing.sold } }
        .filter { listing ->
            parseDateOrNull(listing.dateSold)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
        }

    return PlatformSummary(
        products = sales.size,
        soldAmount = sales.sumOf { parseAmount(it.finalPrice.ifBlank { it.price }) }
    )
}

private fun formatCurrency(amount: Double): String = String.format(Locale.UK, "\u00A3%.2f", amount)
