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
import com.resell.app.data.Product
import com.resell.app.data.ProductFilter
import com.resell.app.data.ProductRepository
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ResellApp(repository: ProductRepository) {
    var products by remember { mutableStateOf(emptyList<Product>()) }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN) }
    var previousScreen by rememberSaveable { mutableStateOf(AppScreen.MAIN) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var productsFilter by rememberSaveable { mutableStateOf(ProductFilter.LISTED) }
    var productsSortNewestFirst by rememberSaveable { mutableStateOf(true) }
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

        AppScreen.SUMMARY -> SummaryScreen(
            products = products,
            onSelectScreen = { screen -> navigateTo(screen) }
        )

        AppScreen.SETTINGS -> SettingsScreen(
            repository = repository,
            onSelectScreen = { screen -> navigateTo(screen) }
        )
    }
}
