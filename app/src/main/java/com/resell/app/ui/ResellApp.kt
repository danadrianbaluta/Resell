package com.resell.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.resell.app.data.AppScreen
import com.resell.app.data.Expense
import com.resell.app.data.Product
import com.resell.app.data.ProductFilter
import com.resell.app.data.ProductRepository
import com.resell.app.data.formatDateForDisplay
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ResellApp(repository: ProductRepository) {
    var products by remember { mutableStateOf(emptyList<Product>()) }
    var expenses by remember { mutableStateOf(emptyList<Expense>()) }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN) }
    var previousScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var productsFilter by rememberSaveable { mutableStateOf(ProductFilter.LISTED) }
    var productsSortNewestFirst by rememberSaveable { mutableStateOf(true) }
    val today = remember { LocalDate.now() }
    var productsStartDate by rememberSaveable { mutableStateOf(formatDateForDisplay(LocalDate.of(2026, 1, 1))) }
    var productsEndDate by rememberSaveable { mutableStateOf(formatDateForDisplay(today)) }
    var productsSelectedYear by rememberSaveable { mutableStateOf(today.year) }
    var productsSelectedMonth by rememberSaveable { mutableStateOf(today.monthValue) }
    val productsGridState = rememberLazyGridState()

    fun navigateTo(screen: AppScreen) {
        if (screen != currentScreen) {
            previousScreen = currentScreen
            currentScreen = screen
        }
    }

    LaunchedEffect(repository) {
        repository.migrateLegacyProducts()
        repository.products.collectLatest { products = it }
    }

    LaunchedEffect(repository) {
        repository.expenses.collectLatest { expenses = it }
    }

    BackHandler(enabled = currentScreen != AppScreen.MAIN) {
        val target = previousScreen
        previousScreen = AppScreen.MAIN
        currentScreen = target
    }

    when (currentScreen) {
        AppScreen.MAIN -> MainScreen(
            products = products,
            filter = productsFilter,
            onFilterChange = { productsFilter = it },
            sortNewestFirst = productsSortNewestFirst,
            onSortNewestFirstChange = { productsSortNewestFirst = it },
            startDate = productsStartDate,
            onStartDateChange = { productsStartDate = it },
            endDate = productsEndDate,
            onEndDateChange = { productsEndDate = it },
            selectedYear = productsSelectedYear,
            onSelectedYearChange = { productsSelectedYear = it },
            selectedMonth = productsSelectedMonth,
            onSelectedMonthChange = { productsSelectedMonth = it },
            gridState = productsGridState,
            onSelectScreen = { screen -> navigateTo(screen) },
            onAdd = {
                selectedProductId = null
                navigateTo(AppScreen.DETAILS)
            },
            onOpenProduct = { productId ->
                selectedProductId = productId
                navigateTo(AppScreen.DETAILS)
            }
        )

        AppScreen.DETAILS -> DetailsScreen(
            existingProduct = products.firstOrNull { it.id == selectedProductId },
            categoryOptions = products
                .map { it.category.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER),
            repository = repository,
            onAfterSave = { product ->
                selectedProductId = product.id
                currentScreen = previousScreen
                previousScreen = AppScreen.MAIN
            },
            onAfterDelete = {
                selectedProductId = null
                currentScreen = previousScreen
                previousScreen = AppScreen.MAIN
            },
            onSelectScreen = { screen -> navigateTo(screen) }
        )

        AppScreen.EXPENSES -> ExpensesScreen(
            expenses = expenses,
            onSelectScreen = { screen -> navigateTo(screen) },
            onAdd = {
                selectedExpenseId = null
                navigateTo(AppScreen.EXPENSE_DETAILS)
            },
            onOpenExpense = { expenseId ->
                selectedExpenseId = expenseId
                navigateTo(AppScreen.EXPENSE_DETAILS)
            }
        )

        AppScreen.EXPENSE_DETAILS -> ExpenseDetailsScreen(
            existingExpense = expenses.firstOrNull { it.id == selectedExpenseId },
            repository = repository,
            onAfterSave = { expense ->
                selectedExpenseId = expense.id
                currentScreen = previousScreen
                previousScreen = AppScreen.MAIN
            },
            onAfterDelete = {
                selectedExpenseId = null
                currentScreen = previousScreen
                previousScreen = AppScreen.MAIN
            },
            onSelectScreen = { screen -> navigateTo(screen) }
        )

        AppScreen.SUMMARY -> SummaryScreen(
            products = products,
            expenses = expenses,
            onSelectScreen = { screen -> navigateTo(screen) }
        )

        AppScreen.SETTINGS -> SettingsScreen(
            repository = repository,
            onSelectScreen = { screen -> navigateTo(screen) }
        )
    }
}
