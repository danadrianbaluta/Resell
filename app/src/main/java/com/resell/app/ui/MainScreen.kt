package com.resell.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.resell.app.data.AppScreen
import com.resell.app.data.createdAtSortDateTime
import com.resell.app.data.formatDateForDisplay
import com.resell.app.data.parseDateOrNull
import com.resell.app.data.Product
import com.resell.app.data.ProductFilter
import com.resell.app.data.matchesFilter
import com.resell.app.data.summaryLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private const val NoStorageFilter = "__NO_STORAGE__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    products: List<Product>,
    filter: ProductFilter,
    onFilterChange: (ProductFilter) -> Unit,
    sortNewestFirst: Boolean,
    onSortNewestFirstChange: (Boolean) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    selectedYear: Int,
    onSelectedYearChange: (Int) -> Unit,
    selectedMonth: Int,
    onSelectedMonthChange: (Int) -> Unit,
    gridState: LazyGridState,
    onSelectScreen: (AppScreen) -> Unit,
    onAdd: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    var filterExpanded by remember { mutableStateOf(false) }
    var dateFilterExpanded by remember { mutableStateOf(false) }
    var storageFilterExpanded by remember { mutableStateOf(false) }
    var selectedStorageLocation by rememberSaveable { mutableStateOf<String?>(null) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val start = parseDateOrNull(startDate) ?: LocalDate.of(2026, 1, 1)
    val end = parseDateOrNull(endDate) ?: today
    val years = remember(today.year) { (today.year - 5..today.year + 2).toList().reversed() }
    val storageLocations = remember(products) {
        products
            .map { it.storageLocation.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val filteredProducts = products
        .filter { it.matchesFilter(filter) }
        .filter { product ->
            product.matchesDateFilter(filter, start, end)
        }
        .filter { product ->
            when (selectedStorageLocation) {
                null -> true
                NoStorageFilter -> product.storageLocation.isBlank()
                else -> product.storageLocation.trim() == selectedStorageLocation
            }
        }
        .let { visibleProducts ->
            if (sortNewestFirst) {
                visibleProducts.sortedByDescending { it.createdAtSortDateTime() ?: java.time.LocalDateTime.MIN }
            } else {
                visibleProducts.sortedBy { it.createdAtSortDateTime() ?: java.time.LocalDateTime.MIN }
            }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = BrandPurple,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add product")
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Products",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { dateFilterExpanded = !dateFilterExpanded },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BrandPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Filter by date",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { storageFilterExpanded = true },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (selectedStorageLocation == null) BrandPurple else BrandOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = "Filter by storage",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = storageFilterExpanded,
                            onDismissRequest = { storageFilterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All storage") },
                                onClick = {
                                    selectedStorageLocation = null
                                    storageFilterExpanded = false
                                    scope.launch { gridState.scrollToItem(0) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("No Storage") },
                                onClick = {
                                    selectedStorageLocation = NoStorageFilter
                                    storageFilterExpanded = false
                                    scope.launch { gridState.scrollToItem(0) }
                                }
                            )
                            storageLocations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location) },
                                    onClick = {
                                        selectedStorageLocation = location
                                        storageFilterExpanded = false
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            onSortNewestFirstChange(!sortNewestFirst)
                            scope.launch { gridState.scrollToItem(0) }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BrandPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (sortNewestFirst) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                            contentDescription = if (sortNewestFirst) "Sort newest to oldest" else "Sort oldest to newest",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Text(filter.summaryLabel(filteredProducts.size), style = MaterialTheme.typography.labelMedium, color = MutedInk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenSelector(current = AppScreen.MAIN, onSelected = onSelectScreen, modifier = Modifier.fillMaxWidth())
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { filterExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BrandPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            ProductFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        onFilterChange(option)
                                        filterExpanded = false
                                        scope.launch { gridState.scrollToItem(0) }
                                    }
                                )
                            }
                        }
                    }
                    }
                }
                if (dateFilterExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DateField(
                                "Start date",
                                startDate,
                                showBorder = true,
                                modifier = Modifier.weight(1f)
                            ) {
                                onStartDateChange(it)
                                scope.launch { gridState.scrollToItem(0) }
                            }
                            DateField(
                                "End date",
                                endDate,
                                showBorder = true,
                                modifier = Modifier.weight(1f)
                            ) {
                                onEndDateChange(it)
                                scope.launch { gridState.scrollToItem(0) }
                            }
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
                                                onSelectedYearChange(year)
                                                yearExpanded = false
                                                val range = YearMonth.of(year, selectedMonth)
                                                onStartDateChange(formatDateForDisplay(range.atDay(1)))
                                                onEndDateChange(
                                                    if (year == today.year && selectedMonth == today.monthValue) {
                                                        formatDateForDisplay(today)
                                                    } else {
                                                        formatDateForDisplay(range.atEndOfMonth())
                                                    }
                                                )
                                                scope.launch { gridState.scrollToItem(0) }
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
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                                onSelectedMonthChange(month)
                                                monthExpanded = false
                                                val range = YearMonth.of(selectedYear, month)
                                                onStartDateChange(formatDateForDisplay(range.atDay(1)))
                                                onEndDateChange(
                                                    if (selectedYear == today.year && month == today.monthValue) {
                                                        formatDateForDisplay(today)
                                                    } else {
                                                        formatDateForDisplay(range.atEndOfMonth())
                                                    }
                                                )
                                                scope.launch { gridState.scrollToItem(0) }
                                            }
                                        )
                                    }
                                }
                            }
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onSelectedYearChange(today.year)
                                    onSelectedMonthChange(today.monthValue)
                                    onStartDateChange(formatDateForDisplay(LocalDate.of(2026, 1, 1)))
                                    onEndDateChange(formatDateForDisplay(today))
                                    scope.launch { gridState.scrollToItem(0) }
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
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onOpenProduct(product.id) })
                }
            }
        }
    }
}

private fun Product.matchesDateFilter(filter: ProductFilter, start: LocalDate, end: LocalDate): Boolean {
    if (filter == ProductFilter.LISTED) {
        return platforms.any { listing ->
            parseDateOrNull(listing.dateListed)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
        }
    }

    if (filter == ProductFilter.SOLD) {
        return platforms.any { listing ->
            parseDateOrNull(listing.dateSold)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
        }
    }

    return createdAtSortDateTime()?.toLocalDate()?.let { !it.isBefore(start) && !it.isAfter(end) } == true
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val listedPlatforms = product.platforms
        .filter { it.dateListed.isNotBlank() || it.price.isNotBlank() }
        .joinToString(", ") { it.platform.label }
    val soldOn = product.platforms.firstOrNull { it.sold }?.platform?.label

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.55f)
                    .background(Brush.linearGradient(listOf(Color(0xFFF5F7FB), Color(0xFFE8EEF9)))),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.description,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MutedInk,
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (soldOn != null) {
                    Text(
                        text = "Sold on $soldOn",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(BrandGreen, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.description.ifBlank { "Untitled product" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (product.purchasePrice.isNotBlank()) {
                        Text(
                            text = "Bought for ${product.purchasePrice}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandOrange
                        )
                    }
                }
                Text(
                    text = if (listedPlatforms.isBlank()) "Listed on: none" else "Listed on: $listedPlatforms",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk
                )
            }
        }
    }
}
